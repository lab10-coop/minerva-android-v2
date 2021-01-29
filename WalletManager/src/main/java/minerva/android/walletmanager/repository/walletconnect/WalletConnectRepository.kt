package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletConnect.client.WCClient

import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.WalletConnectSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface WalletConnectRepository {
    fun connect(
        session: WalletConnectSession,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    )

    fun approveSession(addresses: List<String>, chainId: Int, peerId: String)
    fun rejectSession(peerId: String)
    fun killSession(peerId: String)
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
    val isClientMapEmpty: Boolean
    val walletConnectClients: ConcurrentHashMap<String, WCClient>
    fun getWCSessionFromQr(qrCode: String): WalletConnectSession
    fun getSessionsFlowable(): Flowable<List<DappSession>>
    fun saveDappSession(dappSession: DappSession): Completable
    fun deleteDappSession(peerId: String): Completable
    fun getSessions(): Single<List<DappSession>>
    fun deleteAllDappsForAccount(address: String): Completable
}