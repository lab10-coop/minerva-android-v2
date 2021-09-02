package minerva.android.blockchainprovider.repository.erc721

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.kotlinUtils.map.value
import org.web3j.contracts.eip721.generated.ERC721
import org.web3j.contracts.eip721.generated.ERC721Metadata
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

class ERC721TokenRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>
) : ERC721TokenRepository {

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
        loadERC721(privateKey, chainId, tokenAddress)
            .balanceOf(address)
            .flowable()
            .map { balance -> TokenWithBalance(chainId, tokenAddress, balance.toBigDecimal()) as Token }
            .onErrorReturn { error -> TokenWithError(chainId, tokenAddress, error) }

    override fun getERC721TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        loadERC721Metadata(privateKey, chainId, tokenAddress).name().flowable().toObservable()

    override fun getERC721TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        loadERC721Metadata(privateKey, chainId, tokenAddress).symbol().flowable().toObservable()

    override fun getERC721DetailsUri(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<String> = loadERC721Metadata(privateKey, chainId, tokenAddress).tokenURI(tokenId).flowable().firstOrError()

    private fun loadERC721Metadata(privateKey: String, chainId: Int, address: String) =
        ERC721Metadata.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC721.gasLimit)
        )

    private fun loadERC721(privateKey: String, chainId: Int, address: String) =
        ERC721.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC721.gasLimit)
        )
}
