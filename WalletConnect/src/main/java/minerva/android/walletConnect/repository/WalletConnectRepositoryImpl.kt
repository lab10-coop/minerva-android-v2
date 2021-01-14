package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient
import timber.log.Timber

class WalletConnectRepositoryImpl(private val okHttpClient: OkHttpClient) :
    WalletConnectRepository {

    private lateinit var client: WCClient
    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)


    override fun connect(qrCode: String) {

        with(WCClient(httpClient = okHttpClient)) {

            client = this

            onWCOpen = { peerId ->
                Timber.tag("kobe").d("on wc open, peerId: $peerId")
            }

            onSessionRequest = { id, meta, chainId ->
                Timber.tag("kobe")
                    .d("on session request id: $id; meta: name:${meta.name}, icon: ${meta.icons[0]} url: ${meta.url}, chainID: $chainId")
                status.onNext(OnSessionRequest(meta, chainId))
            }

            onFailure = {
                Timber.tag("kobe").d("on failure: $it")
                status.onNext(OnConnectionFailure(it))
            }

            onDisconnect = { code, reason ->
                Timber.tag("kobe").d("on disconnect: code$code, reason: $reason")
                status.onNext(OnDisconnect(reason))
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