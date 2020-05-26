package minerva.android.blockchainprovider.repository.contract

// don't remove this commented import, please
//import kotlin.Pair
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.contract.ERC20
import minerva.android.blockchainprovider.contract.GnosisSafe
import minerva.android.blockchainprovider.contract.ProxyFactory
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.GNOSIS_SETUP_DATA
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.MASTER_COPY_KEY
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.PROXY_ADDRESS
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.baseGas
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.data
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.gasToken
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.noFunds
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.noGasPrice
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.operation
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.refund
import minerva.android.blockchainprovider.repository.contract.GnosisSafeHelper.safeSentinelAddress
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
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class SmartContractRepositoryImpl(private val web3j: Map<String, Web3j>, private val gasPrice: Map<String, BigInteger>) :
    SmartContractRepository {

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

    override fun getGnosisSafeOwners(contractAddress: String, network: String, privateKey: String): Single<List<String>> =
        getGnosisSafe(contractAddress, network, privateKey).owners.flowable()
            .map { it as List<String> }
            .singleOrError()


    override fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable =
        getGnosisSafe(gnosisAddress, network, privateKey).let { gnosisSafe ->
            Function(
                GnosisSafe.FUNC_ADDOWNERWITHTHRESHOLD,
                listOf<Type<*>>(
                    Address(owner),
                    Uint256(BigInteger.valueOf(1))
                ), emptyList()
            ).let { innerFn ->
                Numeric.hexStringToByteArray(FunctionEncoder.encode(innerFn)).let { data ->
                    return performTransaction(
                        gnosisSafe, gnosisAddress, data, network,
                        TransactionPayload(privateKey = privateKey, contractAddress = gnosisAddress), noFunds
                    )
                }
            }
        }

    override fun removeSafeAccountOwner(removeAddress: String, gnosisAddress: String, network: String, privateKey: String): Completable =
        getGnosisSafeOwners(gnosisAddress, network, privateKey)
            .flatMapCompletable {
                getGnosisSafe(gnosisAddress, network, privateKey).let { gnosisSafe ->
                    Function(
                        GnosisSafe.FUNC_REMOVEOWNER,
                        listOf<Type<*>>(
                            Address(getPreviousOwner(removeAddress, it)),
                            Address(removeAddress),
                            Uint256(BigInteger.valueOf(1))
                        ), emptyList()
                    ).let { innerFn ->
                        performTransaction(
                            gnosisSafe,
                            gnosisAddress,
                            Numeric.hexStringToByteArray(FunctionEncoder.encode(innerFn)),
                            network,
                            TransactionPayload(privateKey = privateKey, contractAddress = gnosisAddress),
                            noFunds
                        )
                    }
                }
            }

    override fun transferERC20Token(network: String, transactionPayload: TransactionPayload, erc20Address: String): Completable {
        Numeric.hexStringToByteArray(getSafeTxData(transactionPayload)).run {
            return performTransaction(getGnosisSafe(transactionPayload, network), erc20Address, this, network, transactionPayload)
        }
    }

    private fun getPreviousOwner(owner: String, owners: List<String>): String {
        owners.forEachIndexed { index, address ->
            if (address == owner.toLowerCase(Locale.getDefault())) {
                return if (index > 0) address else safeSentinelAddress
            }
        }
        throw IllegalArgumentException("Remove Address is not a owner address!")
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
            gasPrice[ARTIS],
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
                noGasPrice,
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

    private fun getSafeTxData(transactionPayload: TransactionPayload): String? {
        Function(
            ERC20.FUNC_TRANSFER, listOf<Type<*>>(
                Address(transactionPayload.receiverKey),
                Uint256(Convert.toWei(transactionPayload.amount, Convert.Unit.ETHER).toBigInteger())
            ), emptyList()
        ).let { innerFn ->
            return FunctionEncoder.encode(innerFn) // returns the abi-encoded hex string
        }
    }

    private fun getGnosisSafe(gnosisAddress: String, network: String, privateKey: String) =
        GnosisSafe.load(
            gnosisAddress, web3j[network], Credentials.create(privateKey),
            ContractGasProvider((gasPrice[network] ?: error("Not supported Network")), Operation.SAFE_ACCOUNT_TXS.gasLimit)
        )

    private fun getGnosisSafe(transactionPayload: TransactionPayload, network: String): GnosisSafe =
        GnosisSafe.load(
            transactionPayload.contractAddress,
            web3j[network],
            Credentials.create(transactionPayload.privateKey),
            ContractGasProvider(Convert.toWei(transactionPayload.gasPrice, Convert.Unit.GWEI).toBigInteger(), transactionPayload.gasLimit)
        )

    private fun getSignatureData(hash: ByteArray, transactionPayload: TransactionPayload): Sign.SignatureData =
        Sign.signMessage(hash, ECKeyPair.create(Numeric.hexStringToByteArray(transactionPayload.privateKey)), false)

    private fun getTransactionHash(receiver: String, gnosisContract: GnosisSafe, amount: BigInteger, nonce: BigInteger, data: ByteArray):
            RemoteCall<ByteArray> =
        gnosisContract.getTransactionHash(receiver, amount, data, operation, safeTxGas, baseGas, noGasPrice, gasToken, refund, nonce)


    private fun getSignedByteArray(signature: Sign.SignatureData): ByteArray = signature.run { r + s + v }

    private fun getGnosisSetupData(address: String) =
        Numeric.hexStringToByteArray(String.format(GNOSIS_SETUP_DATA, address.removePrefix(HEX_PREFIX)))

    private fun getProxyFactory(network: String, privateKey: String): ProxyFactory =
        ProxyFactory.load(
            PROXY_ADDRESS, web3j[network], Credentials.create(privateKey),
            ContractGasProvider((gasPrice[network] ?: error("Not supported Network")), Operation.SAFE_ACCOUNT_TXS.gasLimit)
        )

    companion object {
        private const val HEX_PREFIX = "0x"
        private const val ARTIS = "ATS"
    }
}