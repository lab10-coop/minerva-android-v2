package minerva.android.blockchainprovider.provider

import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object Web3jProvider {

    private const val ENS = 303303303

    fun provideWeb3j(blockchainUrl: MutableMap<Int, String>, ensUrl: String): Map<Int, Web3j> {
        blockchainUrl[ENS] = ensUrl
        return blockchainUrl.mapValues {
            Web3j.build(HttpService(it.value))
        }
    }

    fun provideEnsResolver(ensUrl: String): EnsResolver = EnsResolver(Web3j.build(HttpService(ensUrl)))
}