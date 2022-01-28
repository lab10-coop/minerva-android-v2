package minerva.android.blockchainprovider.repository.erc1155

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.smartContracts.ERC1155
import minerva.android.blockchainprovider.smartContracts.ERC1155Metadata_URI
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.kotlinUtils.map.value
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import java.math.BigInteger

class ERC1155TokenRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>
) : ERC1155TokenRepository {
    override fun getERC1155DetailsUri(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<String> =
        loadERC1155Metadata(privateKey, chainId, tokenAddress).uri(tokenId).flowable()
            .firstOrError()


    override fun getTokenBalance(
        tokenId: String,
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        ownerAddress: String
    ): Flowable<Token> =
        if (ownerAddress.isEmpty()) {
            getBalance(tokenId, tokenAddress, chainId, privateKey, Credentials.create(privateKey).address)
        } else {
            getBalance(tokenId, tokenAddress, chainId, privateKey, ownerAddress)
        }


    private fun getBalance(
        tokenId: String,
        tokenAddress: String,
        chainId: Int,
        privateKey: String,
        ownerAddress: String
    ): Flowable<Token> =
        loadERC1155(privateKey, chainId, tokenAddress)
            .balanceOf(ownerAddress, BigInteger(tokenId))
            .flowable()
            .map { balance ->
                TokenWithBalance(chainId, tokenAddress, balance.toBigDecimal(), tokenId.toString()) as Token
            }
            .onErrorReturn { error -> TokenWithError(chainId, tokenAddress, error, tokenId.toString()) }


    private fun loadERC1155Metadata(privateKey: String, chainId: Int, address: String) =
        ERC1155Metadata_URI.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC1155.gasLimit)
        )


    private fun loadERC1155(privateKey: String, chainId: Int, address: String) =
        ERC1155.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC1155.gasLimit)
        )

    private fun loadERC1155(privateKey: String, chainId: Int, address: String, gasLimit: BigInteger, gasPrice: BigInteger) =
        ERC1155.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                chainId.toLong()
            ),
            ContractGasProvider(gasPrice, gasLimit)
        )


    override fun transferERC1155Token(chainId: Int, payload: TransactionPayload): Completable =
        loadERC1155(payload.privateKey, chainId, payload.contractAddress, payload.gasLimit, Convert.toWei(payload.gasPrice, Convert.Unit.GWEI).toBigInteger())
            .safeTransferFrom(
                payload.senderAddress,
                payload.receiverAddress,
                BigInteger(payload.tokenId),
                CryptoUtils.convertTokenAmount(payload.amount, payload.tokenDecimals),
                NO_ADDITIONAL_DATA
            )
            .flowable()
            .ignoreElements()

    companion object {
        val NO_ADDITIONAL_DATA = byteArrayOf()
    }
}