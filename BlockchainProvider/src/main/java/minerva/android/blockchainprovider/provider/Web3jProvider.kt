package minerva.android.blockchainprovider.provider

import minerva.android.blockchainprovider.BuildConfig
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Async

object Web3jProvider {

    private const val ENS = 303303303
    private const val CHAIN_ID_MATIC = 137
    private const val CHAIN_ID_GNO = 100
    private const val API_KEY_HEADER = "X-API-KEY"

    fun provideWeb3j(blockchainUrls: MutableMap<Int, String>, ensUrl: String): Map<Int, Web3j> {
        blockchainUrls[ENS] = ensUrl

        return blockchainUrls.mapValues { blockchainUrl ->
            Async.run {
                Web3j.build(HttpService(blockchainUrl.value).apply {
                    when (blockchainUrl.key) {
                        CHAIN_ID_MATIC, CHAIN_ID_GNO -> addHeader(API_KEY_HEADER, BuildConfig.X_API_KEY)
                    }
                })
            }.join()
        }
    }

    fun provideEnsResolver(ensUrl: String): EnsResolver =
        Async.run { EnsResolver(Web3j.build(HttpService(ensUrl))) }.join()
}