package minerva.android.apiProvider.api

import io.reactivex.Single
import minerva.android.apiProvider.api.ServicesApi.Companion.APPLICATION_JSON
import minerva.android.apiProvider.api.ServicesApi.Companion.CONTENT_TYPE
import minerva.android.apiProvider.model.*
import minerva.android.apiProvider.model.gaswatch.GasPrices
import retrofit2.http.*

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

    @GET
    fun getLastCommitFromTokenList(@Url url: String): Single<List<CommitElement>>

    @GET
    fun getConnectedTokens(@Url url: String): Single<TokenBalanceResponse>

    @GET
    fun getTokenTx(@Url url: String): Single<TokenTxResponse>

    @GET(CHAIN_DETAILS_URL)
    fun getChainDetails(): Single<List<ChainDetails>>

    @GET
    fun getERC721TokenDetails(@Url url: String): Single<ERC721Details>

    companion object {
        private const val ID = "id"
        private const val IDS = "ids"
        private const val VS_CURRENCIES = "vs_currencies"
        private const val CONTRACT_ADDRESSES = "contract_addresses"
        private const val CHAIN_DETAILS_URL = "https://chainlist.org/chains.json"
    }
}