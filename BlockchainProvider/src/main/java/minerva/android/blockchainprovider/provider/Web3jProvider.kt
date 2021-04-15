package minerva.android.blockchainprovider.provider

import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Async

object Web3jProvider {

    private const val ENS = 303303303

    fun provideWeb3j(blockchainUrl: MutableMap<Int, String>, ensUrl: String): Map<Int, Web3j> {
        blockchainUrl[ENS] = ensUrl

        return blockchainUrl.mapValues {
            Async.run { Web3j.build(HttpService(it.value)) }.join()
        }
    }

    fun provideEnsResolver(ensUrl: String): EnsResolver {
        return Async.run { EnsResolver(Web3j.build(HttpService(ensUrl))) }.join()
    }

}