package minerva.android.blockchainprovider.repository.erc20

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.smartContracts.ERC20
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.kotlinUtils.map.value
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

class ERC20TokenRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>
) : ERC20TokenRepository {

    override fun getTokenBalance(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        safeAccountAddress: String
    ): Flowable<Token> =
        if (safeAccountAddress.isEmpty()) {
            getBalance(tokenAddress, chainId, privateKey, Credentials.create(privateKey).address)
        } else {
            getBalance(tokenAddress, chainId, privateKey, safeAccountAddress)
        }

    private fun getBalance(
        tokenAddress: String,
        chainId: Int,
        privateKey: String,
        address: String
    ): Flowable<Token> =
        loadERC20(privateKey, chainId, tokenAddress)
            .balanceOf(address)
            .flowable()
            .map { balance -> TokenWithBalance(chainId, tokenAddress, balance.toBigDecimal()) as Token }
            .onErrorReturn { error -> TokenWithError(chainId, tokenAddress, error) }

    override fun getERC20TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        loadERC20(privateKey, chainId, tokenAddress).name().flowable().toObservable()

    override fun getERC20TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        loadERC20(privateKey, chainId, tokenAddress).symbol().flowable().toObservable()

    override fun getERC20TokenDecimals(
        privateKey: String,
        chainId: Int,
        tokenAddress: String
    ): Observable<BigInteger> = loadERC20(privateKey, chainId, tokenAddress).decimals().flowable().toObservable()

    override fun transferERC20Token(chainId: Int, payload: TransactionPayload): Completable =
        loadTransactionERC20(payload.privateKey, chainId, payload.contractAddress, payload)
            .transfer(
                payload.receiverAddress,
                CryptoUtils.convertTokenAmount(payload.amount, payload.tokenDecimals)
            )
            .flowable()
            .ignoreElements()

    private fun loadERC20(privateKey: String, chainId: Int, address: String) =
        ERC20.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC20.gasLimit)
        )

    //todo probably can be removed and replaced with the sendTransaction method
    private fun loadTransactionERC20(
        privateKey: String,
        chainId: Int,
        address: String,
        payload: TransactionPayload
    ) = ERC20.load(
        address, web3j.value(chainId),
        RawTransactionManager(
            web3j.value(chainId),
            Credentials.create(privateKey),
            chainId.toLong()
        ),
        ContractGasProvider(payload.gasPriceWei, payload.gasLimit)
    )
}