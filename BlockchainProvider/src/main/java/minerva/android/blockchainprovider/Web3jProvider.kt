package minerva.android.blockchainprovider

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object Web3jProvider {
    fun provideWeb3j(blockchainUrl: String): Web3j = Web3j.build(HttpService(blockchainUrl))
}