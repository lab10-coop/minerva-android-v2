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
import minerva.android.apiProvider.model.GasPricesMatic
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.NftCollectionDetails
import minerva.android.apiProvider.model.RpcOverHttpPayload
import minerva.android.apiProvider.model.TokenBalanceResponse
import minerva.android.apiProvider.model.TokenDetails
import minerva.android.apiProvider.model.TokenTxResponse
import minerva.android.apiProvider.model.TokensOwnedPayload
import minerva.android.apiProvider.model.gaswatch.GasPrices
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface CryptoApi {

    @GET("simple/price")
    fun getMarkets(@Query(IDS) coinIds: String, @Query(VS_CURRENCIES) currency: String): Single<Markets>

    @GET("simple/token_price/{$ID}")
    fun getTokensRate(
        @Path(ID) chainId: String,
        @Query(CONTRACT_ADDRESSES) contractAddress: String,
        @Query(VS_CURRENCIES) currency: String
    ): Single<Map<String, Map<String, String>>>

    @GET
    fun getGasPrice(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Url url: String
    ): Single<GasPrices>

    @GET
    fun getGasPriceForMatic(
        @Url url: String
    ): Single<GasPricesMatic>

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
    fun getLastCommitFromTokenList(@Url url: String): Single<List<CommitElement>>

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

    @GET(DAPPS_DETAILS_LAST_COMMIT)
    fun getLastCommitFromDappsDetails(): Single<List<CommitElement>>

    companion object {
        private const val ID = "id"
        private const val IDS = "ids"
        private const val VS_CURRENCIES = "vs_currencies"
        private const val CONTRACT_ADDRESSES = "contract_addresses"
        private const val CHAIN_DETAILS_URL = "https://chainlist.org/chains.json"
        private const val NFT_COLLECTION_DETAILS_URL =
            "https://raw.githubusercontent.com/lab10-coop/minerva-nft-list/main/all.json"
        private const val DAPPS_DETAILS_LAST_COMMIT =
            "https://api.github.com/repos/lab10-coop/minerva-dapplist/commits?path=dapplist.json&page=1&per_page=1"
    }
}