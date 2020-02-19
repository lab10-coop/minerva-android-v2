package minerva.android.blockchainprovider

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object Web3jProvider {
    fun provideWeb3j(blockchainUrl: Map<String, String>): Map<String, Web3j> =
        blockchainUrl.mapValues { Web3j.build(HttpService(it.value)) }
}