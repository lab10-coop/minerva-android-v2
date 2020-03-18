package minerva.android.blockchainprovider.repository.contract

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.contract.ERC20
import minerva.android.blockchainprovider.contract.GnosisSafe
import minerva.android.blockchainprovider.contract.ProxyFactory
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.GNOSIS_SETUP_DATA
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.MASTER_COPY_KEY
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.PROXY_ADDRESS
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.provider.SmartContractGasProvider
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.baseGas
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.data
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.gasPrice
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.gasToken
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.noFunds
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.operation
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.refund
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.safeTxGas
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.utils.Numeric
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import kotlin.Pair
//import kotlin.Pair
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.utils.Convert
import java.math.BigInteger

class SmartContractRepositoryImpl(private val web3j: Map<String, Web3j>) : SmartContractRepository {

    override fun deployGnosisSafeContract(privateKey: String, address: String, network: String): Single<String> =
        getProxyFactory(network, privateKey)
            .createProxy(MASTER_COPY_KEY, getGnosisSetupData(address))
            .flowable()
            .singleOrError()
            .flatMap { getProxyFactory(network, privateKey).getProxyCreationEvents(it).toFlowable().singleOrError() }
            .map { it.proxy }

    override fun transferNativeCoin(network: String, transactionPayload: TransactionPayload): Completable {
        Convert.toWei(transactionPayload.amount, Convert.Unit.ETHER).toBigInteger().run {
            return performTransaction(
                getGnosisSafe(transactionPayload, network),
                transactionPayload.receiverKey, data, network, transactionPayload, this
            )
        }
    }

    override fun getGnosisSafeOwners(gnosisAddress: String, network: String, privateKey: String): Single<List<String>> =
        getGnosisSafe(gnosisAddress, network, privateKey).owners.flowable()
            .map { it as List<String> }
            .singleOrError()


    override fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable {
        val data: ByteArray = Numeric.hexStringToByteArray(
            getGnosisSafe(gnosisAddress, network, privateKey).addOwnerWithThreshold(owner, ADD_OWNER_THRESHOLD).encodeFunctionCall()
        )

        getGnosisSafe(gnosisAddress, network, privateKey).let { gnosisSafe ->
            return performTransaction(
                gnosisSafe, gnosisAddress, data, network,
                TransactionPayload(privateKey = privateKey, contractAddress = gnosisAddress), noFunds
            )
        }
    }


    override fun transferERC20Token(network: String, transactionPayload: TransactionPayload, erc20Address: String): Completable {
        Numeric.hexStringToByteArray(getSafeTxData(getERC20(erc20Address, network, transactionPayload), transactionPayload)).run {
            return performTransaction(getGnosisSafe(transactionPayload, network), erc20Address, this, network, transactionPayload)
        }
    }

    private fun performTransaction(
        gnosisSafe: GnosisSafe,
        receiver: String,
        signedData: ByteArray,
        network: String,
        transactionPayload: TransactionPayload,
        amount: BigInteger = BigInteger.valueOf(0)
    ): Completable =
        gnosisSafe.nonce().flowable()
            .flatMapCompletable { nonce ->
                getTransactionHash(receiver, gnosisSafe, amount, nonce, signedData)
                    .flowable()
                    .zipWith(
                        (web3j[network] ?: error("Not supported Network! ($network)"))
                            .ethGetTransactionCount(
                                Credentials.create(transactionPayload.privateKey).address,
                                DefaultBlockParameterName.LATEST
                            ).flowable()

                    ).flatMapCompletable { hashAndCount ->
                        (web3j[network] ?: error("Not supported Network! ($network)"))
                            .ethSendRawTransaction(getSignedTransaction(receiver, transactionPayload, amount, hashAndCount, signedData))
                            .flowable()
                            .flatMapCompletable { transaction ->
                                if (transaction.error == null) Completable.complete()
                                else Completable.error(Throwable())
                            }
                    }
            }



    private fun getSignedTransaction(
        receiver: String,
        transactionPayload: TransactionPayload,
        amount: BigInteger,
        hashAndCount: Pair<ByteArray, EthGetTransactionCount>,
        data: ByteArray
    ): String =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                getRawTransaction(
                    receiver,
                    transactionPayload,
                    hashAndCount.second.transactionCount,
                    amount,
                    getSignatureData(hashAndCount.first, transactionPayload),
                    data
                ), Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun getRawTransaction(
        receiver: String,
        transactionPayload: TransactionPayload,
        count: BigInteger,
        amount: BigInteger,
        sig: Sign.SignatureData,
        data: ByteArray
    ): RawTransaction =
        RawTransaction.createTransaction(
            count,
            SmartContractGasProvider().gasPrice,
            safeTxGas + baseGas,
            transactionPayload.contractAddress,
            getTxFromTransactionFunction(receiver, amount, sig, data)
        )

    private fun getTxFromTransactionFunction(receiver: String, amount: BigInteger, sig: Sign.SignatureData, data: ByteArray): String =
        FunctionEncoder.encode(
            getExecutionTransactionFunction(
                receiver,
                amount,
                data,
                operation,
                safeTxGas,
                baseGas,
                gasPrice,
                getSignedByteArray(sig)
            )
        )

    private fun getExecutionTransactionFunction(
        receiver: String,
        amount: BigInteger,
        data: ByteArray,
        operation: BigInteger,
        safeTxGas: BigInteger,
        baseGas: BigInteger,
        gasPrice: BigInteger,
        sigByteArr: ByteArray
    ): Function =
        Function(
            GnosisSafe.FUNC_EXECTRANSACTION,
            listOf<Type<*>>(
                Address(receiver),
                Uint256(amount),
                DynamicBytes(data),
                Uint8(operation),
                Uint256(safeTxGas),
                Uint256(baseGas),
                Uint256(gasPrice),
                Address(gasToken),
                Address(refund),
                DynamicBytes(sigByteArr)
            ), emptyList()
        )

    private fun getSafeTxData(erc20: ERC20, transactionPayload: TransactionPayload) =
        erc20.transfer(transactionPayload.receiverKey, Convert.toWei(transactionPayload.amount, Convert.Unit.ETHER).toBigInteger())
            .encodeFunctionCall()

    private fun getERC20(erc20Address: String, network: String, transactionPayload: TransactionPayload): ERC20 =
        ERC20.load(
            erc20Address,
            web3j[network],
            ReadonlyTransactionManager(web3j[network], transactionPayload.contractAddress),
            ContractGasProvider(Convert.toWei(transactionPayload.gasPrice, Convert.Unit.GWEI).toBigInteger(), transactionPayload.gasLimit)
        )

    private fun getGnosisSafe(gnosisAddress: String, network: String, privateKey: String) =
        GnosisSafe.load(gnosisAddress, web3j[network], Credentials.create(privateKey), SmartContractGasProvider())

    private fun getGnosisSafe(transactionPayload: TransactionPayload, network: String): GnosisSafe =
        GnosisSafe.load(
            transactionPayload.contractAddress,
            web3j[network],
            Credentials.create(transactionPayload.privateKey),
            ContractGasProvider(Convert.toWei(transactionPayload.gasPrice, Convert.Unit.GWEI).toBigInteger(), transactionPayload.gasLimit)
        )

    private fun getSignatureData(hash: ByteArray, transactionPayload: TransactionPayload): Sign.SignatureData =
        Sign.signMessage(hash, ECKeyPair.create(Numeric.hexStringToByteArray(transactionPayload.privateKey)), false)

    private fun getTransactionHash(
        receiver: String,
        gnosisContract: GnosisSafe,
        amount: BigInteger,
        nonce: BigInteger,
        data: ByteArray
    ): RemoteFunctionCall<ByteArray> =
        gnosisContract
            .getTransactionHash(receiver, amount, data, operation, safeTxGas, baseGas, gasPrice, gasToken, refund, nonce)


    private fun getSignedByteArray(signature: Sign.SignatureData): ByteArray =
        signature.run { r + s + v }

    private fun getGnosisSetupData(address: String) =
        Numeric.hexStringToByteArray(String.format(GNOSIS_SETUP_DATA, address.removePrefix(HEX_PREFIX)))

    private fun getProxyFactory(network: String, privateKey: String): ProxyFactory =
        ProxyFactory.load(PROXY_ADDRESS, web3j[network], Credentials.create(privateKey), SmartContractGasProvider())

    companion object {
        private const val HEX_PREFIX = "0x"
        private val ADD_OWNER_THRESHOLD = BigInteger.valueOf(1)
    }
}