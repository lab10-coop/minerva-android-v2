package minerva.android.walletConnect.repository

import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient
import timber.log.Timber

class WalletConnectRepositoryImpl(private val okHttpClient: OkHttpClient) :
    WalletConnectRepository {

    private lateinit var client: WCClient

    override fun connect(qrCode: String) {

        with(WCClient(httpClient = okHttpClient)) {

            client = this

            onWCOpen = { peerId ->
                Timber.tag("kobe").d("on wc open, peerId: $peerId")
            }

            onSessionRequest = { id, peer, chainId ->
                Timber.tag("kobe")
                    .d("on session request id: $id; meta: name:${peer.name}, icon: ${peer.icons[0]} url: ${peer.url}, chainID: $chainId")
            }

            onFailure = {
                Timber.tag("kobe").d("on failure: $it")
            }

            onDisconnect = { code, reason ->
                Timber.tag("kobe").d("on disconnect: code$code, reason: $reason")
            }

            WCSession.from(qrCode)?.let {
                connect(
                    it,
                    peerMeta = WCPeerMeta( //todo extract values
                        name = "Minerva Wallet",
                        url = "https://docs.minerva.digital/",
                        description = "Minerva Wallet"
                    )
                )
            }
        }
    }

    override fun approve() {
        TODO("approve connection")
    }

    override fun close() {
        client.disconnect()
    }

}