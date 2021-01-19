package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient
import timber.log.Timber

//todo tests will be added when multiple session management is implemented
class WalletConnectRepositoryImpl(private val okHttpClient: OkHttpClient) :
    WalletConnectRepository {

    //TODO manage all sessions/connected dApps for given account

    private lateinit var client: WCClient

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)



    override fun connect(qrCode: String) {

        with(WCClient(httpClient = okHttpClient)) {

            client = this
            onWCOpen = { peerId ->
                Timber.tag("kobe").d("peerId: $peerId")
                //todo handle for multiple session management
            }

            onSessionRequest = { id, meta, chainId ->
                status.onNext(OnSessionRequest(meta, chainId))
            }

            onFailure = {
                status.onNext(OnConnectionFailure(it))
            }

            onDisconnect = { code, reason ->
                status.onNext(OnDisconnect(code))
            }

            WCSession.from(qrCode)?.let {

                Timber.tag("kobe").d("topic: ${it.topic}")

                connect(
                    it,
                    peerMeta = WCPeerMeta( //todo extract valuesz
                        name = "Minerva Wallet",
                        url = "https://docs.minerva.digital/"
                    )
                )
            }
        }
    }

    override fun approveSession(addresses: List<String>, chainId: Int) {
        client.approveSession(addresses, chainId)
    }

    override fun rejectSession() {
        client.rejectSession()
    }

    override fun killSession() {
        client.killSession()
    }

    override fun disconnect() {
        client.disconnect()
    }

}