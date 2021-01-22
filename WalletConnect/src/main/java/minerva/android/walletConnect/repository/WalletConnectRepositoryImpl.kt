package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.database.WalletConnectDatabase
import minerva.android.walletConnect.mapper.DappSessionToEntityMapper
import minerva.android.walletConnect.mapper.EntityToDappSessionMapper
import minerva.android.walletConnect.model.session.DappSession
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

//todo move to service binded to MainActivity
class WalletConnectRepositoryImpl(
    private val okHttpClient: OkHttpClient,
    database: WalletConnectDatabase
) : WalletConnectRepository {

    private val dappDao = database.dappDao()

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()

    override fun connect(session: WCSession, peerId: String, remotePeerId: String?) {

        with(WCClient(httpClient = okHttpClient)) {

            onWCOpen = { peerId ->
                Timber.tag("kobe").d("ON OPEN peerId: $peerId")
                clientMap[peerId] = this
            }

            onSessionRequest = { remotePeerId, meta, chainId, peerId ->
                Timber.tag("kobe")
                    .d("ON SESSION REQUEST remotePeerId: $remotePeerId; peerId: $peerId")
                status.onNext(OnSessionRequest(meta, chainId, Topic(peerId, remotePeerId)))
            }

            onFailure = {
                status.onNext(OnConnectionFailure(it))
            }

            onEthSign = { id, _ ->
                Timber.tag("kobe").d("ON ETH SIGN id: $id")
            }

            onDisconnect = { code, reason, peerId ->
                status.onNext(OnDisconnect(code, peerId))
            }

            connect(
                session,
                peerMeta = WCPeerMeta( //todo extract values
                    name = "Minerva Wallet",
                    url = "https://docs.minerva.digital/"
                ),
                peerId = peerId,
                remotePeerId = remotePeerId
            )

        }
    }

    override fun getConnectedDapps(addresses: String): Flowable<List<DappSession>> =
        dappDao.getConnectedDapps(addresses)
            .map { EntityToDappSessionMapper.map(it) }

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String) {
        clientMap[peerId]?.approveSession(addresses, chainId, peerId)
        Timber.tag("kobe").d("approved PEER ID: $peerId")
    }

    override fun rejectSession(peerId: String) {
        clientMap[peerId]?.rejectSession()
        Timber.tag("kobe").d("reject PEER ID: $peerId")
    }

    override fun killSession(peerId: String) {
        clientMap[peerId]?.killSession()
        Timber.tag("kobe").d("kill session PEER ID: $peerId")
    }
}