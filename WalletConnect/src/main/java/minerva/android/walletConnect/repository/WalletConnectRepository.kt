package minerva.android.walletConnect.repository

import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.walletConnect.client.WalletConnectStatus
import minerva.android.walletConnect.model.session.DappSession
import minerva.android.walletConnect.model.session.WCSession
import java.util.*

interface WalletConnectRepository {
    fun connect(
        session: WCSession,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    )

    fun approveSession(addresses: List<String>, chainId: Int, peerId: String)
    fun rejectSession(peerId: String)
    fun killSession(peerId: String)
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
    fun getConnectedDapps(addresses: String): Flowable<List<DappSession>>
    fun saveDappSession(dappSession: DappSession): Completable
    fun deleteDappSession(peerId: String): Completable
}