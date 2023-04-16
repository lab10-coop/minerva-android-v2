package minerva.android.apiProvider.api

import io.reactivex.Single
import minerva.android.apiProvider.BuildConfig
import minerva.android.apiProvider.api.ServicesApi.Companion.APPLICATION_JSON
import minerva.android.apiProvider.api.ServicesApi.Companion.CONTENT_TYPE
import minerva.android.apiProvider.model.ChainDetails
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.DappDetailsList
import minerva.android.apiProvider.model.NftDetails
import minerva.android.apiProvider.model.GasPricesFromRpcOverHttp
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.NftCollectionDetails
import minerva.android.apiProvider.model.RpcOverHttpPayload
import minerva.android.apiProvider.model.TokenBalanceResponse
import minerva.android.apiProvider.model.TokenDetails
import minerva.android.apiProvider.model.TokenTxResponse
import minerva.android.apiProvider.model.TokensOwnedPayload
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface CryptoApi {

    @GET("simple/price")
    fun getMarkets(@Query(IDS) coinIds: String, @Query(VS_CURRENCIES) currency: String): Single<Markets>

    @GET(TOKENPRICE_API_URL)
    fun getTokensRate(
        @Query(CHAIN_ID) chainId: String,
        @Query(CONTRACT_ADDRESSES) contractAddress: String,
        @Query(VS_CURRENCIES) currency: String
    ): Single<Map<String, Map<String, String>>>

    @POST
    fun getGasPriceFromRpcOverHttp(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Url url: String,
        @Body rpcOverHttpPayload: RpcOverHttpPayload = RpcOverHttpPayload.GET_GAS_PRICE_PAYLOAD
    ): Single<GasPricesFromRpcOverHttp>

    @GET
    fun getTokenDetails(@Url url: String): Single<List<TokenDetails>>

    @GET(NFT_COLLECTION_DETAILS_URL)
    fun getNftCollectionDetails(): Single<List<NftCollectionDetails>>

    @GET
    fun getConnectedTokens(@Url url: String): Single<TokenBalanceResponse>

    @GET
    fun getTokensOwned(@Url url: String): Single<TokensOwnedPayload>

    @GET
    fun getTokenTx(@Url url: String): Single<TokenTxResponse>

    @GET(CHAIN_DETAILS_URL)
    fun getChainDetails(): Single<List<ChainDetails>>

    @GET
    fun getERC721TokenDetails(@Url url: String): Single<NftDetails>

    @GET
    fun getERC1155TokenDetails(@Url url: String): Single<NftDetails>

    @GET(BuildConfig.DAPPS_DETAILS_URL)
    fun getDappsDetails(): Single<DappDetailsList>

    @GET(BuildConfig.DAPPS_DETAILS_LAST_COMMIT)
    fun getLastCommitFromDappsDetails(): Single<List<CommitElement>>

    companion object {
        private const val IDS = "ids"
        private const val CHAIN_ID = "chainId"
        private const val CONTRACT_ADDRESSES = "contract_addresses"
        private const val VS_CURRENCIES = "vs_currencies"
        private const val CHAIN_DETAILS_URL = "https://chainid.network/chains_mini.json"
        private const val NFT_COLLECTION_DETAILS_URL =
            "https://raw.githubusercontent.com/lab10-coop/minerva-nft-list/main/all.json"
        private const val TOKENPRICE_API_URL = "https://tokensowned-api.minerva.digital/eth/v1/tokenprice"
    }
}