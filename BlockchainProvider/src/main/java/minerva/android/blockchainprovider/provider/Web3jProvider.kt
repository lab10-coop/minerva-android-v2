package minerva.android.blockchainprovider.provider

import minerva.android.blockchainprovider.defs.BlockchainDef.Companion.ENS
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import java.net.URI

object Web3jProvider {

    fun provideWeb3j(blockchainUrl: MutableMap<String, String>, ensUrl: String): Map<String, Web3j> {
        blockchainUrl[ENS] = ensUrl
        return blockchainUrl.mapValues {
            Web3j.build(HttpService(it.value))
        }
    }
    fun provideEnsResolver(ensUrl: String): EnsResolver = EnsResolver(Web3j.build(HttpService(ensUrl)))
}