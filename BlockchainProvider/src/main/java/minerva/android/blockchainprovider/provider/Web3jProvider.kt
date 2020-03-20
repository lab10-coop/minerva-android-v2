package minerva.android.blockchainprovider.provider

import minerva.android.blockchainprovider.defs.BlockchainDef.Companion.ENS
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object Web3jProvider {
    fun provideWeb3j(blockchainUrl: MutableMap<String, String>, ensUrl: String): Map<String, Web3j> {
        blockchainUrl[ENS] = ensUrl
        return blockchainUrl.mapValues {
            Web3j.build(HttpService(it.value)) }
    }
}