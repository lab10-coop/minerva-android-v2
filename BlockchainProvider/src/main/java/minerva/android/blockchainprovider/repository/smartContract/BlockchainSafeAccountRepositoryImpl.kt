package minerva.android.blockchainprovider.repository.smartContract

// don't remove this commented import, please
//import kotlin.Pair
import kotlin.Pair
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.GNOSIS_SETUP_DATA
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.MASTER_COPY_KEY
import minerva.android.blockchainprovider.defs.SmartContractConstants.Companion.PROXY_ADDRESS
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.baseGas
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.data
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.gasToken
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.noFunds
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.noGasPrice
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.operation
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.refund
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.safeSentinelAddress
import minerva.android.blockchainprovider.repository.smartContract.GnosisSafeHelper.safeTxGas
import minerva.android.blockchainprovider.smartContracts.ERC20
import minerva.android.blockchainprovider.smartContracts.GnosisSafe
import minerva.android.blockchainprovider.smartContracts.ProxyFactory
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.map.value
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.NetVersion
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*


class BlockchainSafeAccountRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrice: Map<Int, BigInteger>
) : BlockchainSafeAccountRepository {

    override fun deployGnosisSafeContract(privateKey: String, address: String, chainId: Int): Single<String> =
        getProxyFactory(chainId, privateKey)
            .createProxy(MASTER_COPY_KEY, getGnosisSetupData(address))
            .flowable()
            .singleOrError()
            .flatMap { getProxyFactory(chainId, privateKey).getProxyCreationEvents(it).toFlowable().singleOrError() }
            .map { it.proxy }

    override fun transferNativeCoin(chainId: Int, transactionPayload: TransactionPayload): Completable {
        return performTransaction(
            getGnosisSafe(transactionPayload, chainId),
            transactionPayload.receiverAddress, data, chainId, transactionPayload,
            Convert.toWei(transactionPayload.amount, Convert.Unit.ETHER).toBigInteger()
        )
    }

    override fun getGnosisSafeOwners(contractAddress: String, chainId: Int, privateKey: String): Single<List<String>> =
        getGnosisSafe(contractAddress, chainId, privateKey).owners.flowable()
            .map { it as List<String> }
            .singleOrError()


    override fun addSafeAccountOwner(
        owner: String,
        gnosisAddress: String,
        chainId: Int,
        privateKey: String
    ): Completable =
        try {
            val gnosisSafe = getGnosisSafe(gnosisAddress, chainId, privateKey)
            val result = gnosisSafe.addOwnerWithThreshold(owner, BigInteger.valueOf(1)).encodeFunctionCall()
            val data = Numeric.hexStringToByteArray(result)
            performTransaction(
                gnosisSafe, gnosisAddress, data, chainId,
                TransactionPayload(privateKey = privateKey, contractAddress = gnosisAddress), noFunds
            )
        } catch (ex: NumberFormatException) {
            Completable.error(ex)
        }

    override fun removeSafeAccountOwner(
        removeAddress: String,
        gnosisAddress: String,
        chainId: Int,
        privateKey: String
    ): Completable = getGnosisSafeOwners(gnosisAddress, chainId, privateKey)
        .flatMapCompletable {
            getGnosisSafe(gnosisAddress, chainId, privateKey).let { gnosisSafe ->
                gnosisSafe.removeOwner(getPreviousOwner(removeAddress, it), removeAddress, BigInteger.valueOf(1))
                    .encodeFunctionCall()
                    .let { data ->
                        performTransaction(
                            gnosisSafe, gnosisAddress, Numeric.hexStringToByteArray(data), chainId,
                            TransactionPayload(privateKey = privateKey, contractAddress = gnosisAddress), noFunds
                        )
                    }
            }
        }

    override fun transferERC20Token(
        chainId: Int,
        transactionPayload: TransactionPayload,
        tokenAddress: String
    ): Completable {
        Numeric.hexStringToByteArray(getSafeTxData(transactionPayload)).run {
            return performTransaction(
                getGnosisSafe(transactionPayload, chainId),
                tokenAddress,
                this,
                chainId,
                transactionPayload
            )
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
        chainId: Int,
        transactionPayload: TransactionPayload,
        amount: BigInteger = BigInteger.valueOf(0)
    ): Completable = gnosisSafe.nonce().flowable().flatMapCompletable { nonce ->
        getTransactionHash(receiver, gnosisSafe, amount, nonce, signedData)
            .flowable()
            .zipWith(
                web3j.value(chainId).ethGetTransactionCount(
                    Credentials.create(transactionPayload.privateKey).address,
                    DefaultBlockParameterName.LATEST
                ).flowable()
            )
            .flatMapCompletable {
                web3j.value(chainId).ethSendRawTransaction(
                    getSignedTransaction(
                        gnosisSafe,
                        receiver,
                        transactionPayload,
                        amount,
                        it,
                        chainId.toLong(),
                        signedData
                    )
                )
                    .flowable()
                    .flatMapCompletable { transaction ->
                        if (transaction.error == null) Completable.complete()
                        else Completable.error(Throwable(transaction.error.message))
                    }
            }
    }

    private fun getSignedTransaction(
        gnosisSafe: GnosisSafe,
        receiver: String,
        transactionPayload: TransactionPayload,
        amount: BigInteger,
        hashAndCount: Pair<ByteArray, EthGetTransactionCount>,
        chainId: Long,
        data: ByteArray
    ): String =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                getRawTransaction(
                    gnosisSafe,
                    receiver,
                    transactionPayload,
                    hashAndCount.second.transactionCount,
                    amount,
                    getSignatureData(hashAndCount.first, transactionPayload),
                    data
                ), chainId, Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun getRawTransaction(
        gnosisSafe: GnosisSafe,
        receiver: String,
        transactionPayload: TransactionPayload,
        count: BigInteger,
        amount: BigInteger,
        sig: Sign.SignatureData,
        data: ByteArray
    ): RawTransaction =
        RawTransaction.createTransaction(
            count,
            transactionPayload.gasPriceWei,
            safeTxGas + baseGas,
            transactionPayload.contractAddress,
            getTxFromTransaction(gnosisSafe, receiver, amount, sig, data)
        )

    private fun getTxFromTransaction(
        gnosisSafe: GnosisSafe, receiver:
        String, amount: BigInteger, sig:
        Sign.SignatureData, data: ByteArray
    ): String =
        gnosisSafe.execTransaction(
            receiver, amount, data, operation,
            safeTxGas, baseGas, noGasPrice, gasToken, refund, getSignedByteArray(sig)
        ).encodeFunctionCall()

    private fun getSafeTxData(transactionPayload: TransactionPayload): String? {
        Function(
            ERC20.FUNC_TRANSFER, listOf<Type<*>>(
                Address(transactionPayload.receiverAddress),
                Uint256(Convert.toWei(transactionPayload.amount, Convert.Unit.ETHER).toBigInteger())
            ), emptyList()
        ).let { innerFn ->
            return FunctionEncoder.encode(innerFn) // returns the abi-encoded hex string
        }
    }

    private fun getGnosisSafe(gnosisAddress: String, chainId: Int, privateKey: String) = GnosisSafe.load(
        gnosisAddress, web3j.value(chainId), Credentials.create(privateKey),
        ContractGasProvider(gasPrice.value(chainId), Operation.SAFE_ACCOUNT_TXS.gasLimit)
    )

    private fun getGnosisSafe(transactionPayload: TransactionPayload, chainId: Int): GnosisSafe =
        GnosisSafe.load(
            transactionPayload.contractAddress,
            web3j.value(chainId),
            Credentials.create(transactionPayload.privateKey),
            ContractGasProvider(
                Convert.toWei(transactionPayload.gasPrice, Convert.Unit.GWEI).toBigInteger(),
                transactionPayload.gasLimit
            )
        )

    private fun getSignatureData(hash: ByteArray, transactionPayload: TransactionPayload): Sign.SignatureData =
        Sign.signMessage(hash, ECKeyPair.create(Numeric.hexStringToByteArray(transactionPayload.privateKey)), false)

    private fun getTransactionHash(
        receiver: String,
        gnosisContract: GnosisSafe,
        amount: BigInteger,
        nonce: BigInteger,
        data: ByteArray
    ): RemoteCall<ByteArray> = gnosisContract.getTransactionHash(
        receiver,
        amount,
        data,
        operation,
        safeTxGas,
        baseGas,
        noGasPrice,
        gasToken,
        refund,
        nonce
    )

    private fun getSignedByteArray(signature: Sign.SignatureData): ByteArray = signature.run { r + s + v }

    private fun getGnosisSetupData(address: String) =
        Numeric.hexStringToByteArray(String.format(GNOSIS_SETUP_DATA, address.removePrefix(HEX_PREFIX)))

    private fun getProxyFactory(chainId: Int, privateKey: String): ProxyFactory = ProxyFactory.load(
        PROXY_ADDRESS, web3j.value(chainId), Credentials.create(privateKey),
        ContractGasProvider(gasPrice.value(chainId), Operation.SAFE_ACCOUNT_TXS.gasLimit)
    )
}