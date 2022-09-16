package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import java.util.*

interface WalletConnectRepository {
    fun connect(
        session: WalletConnectSession,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null,
        dapps: List<DappSession> = emptyList()
    )

    fun approveSession(addresses: List<String>, chainId: Int, peerId: String, dapp: DappSession): Completable

    /**
     * Update Session - update current wallet connection
     * @param connectionPeerId - id of socket_client connection
     * @param accountAddress - address of account which was specified
     * @param accountChainId - chainId of account which was specified
     * @param accountName - name (with index) of account which was specified
     * @return Completable
     */
    fun updateSession(connectionPeerId: String, accountAddress: String, accountChainId: Int, accountName: String): Completable
    fun rejectSession(peerId: String)
    fun killSession(peerId: String): Completable
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
    fun getWCSessionFromQr(qrCode: String): WalletConnectSession
    fun getSessionsFlowable(): Flowable<List<DappSession>>
    fun saveDappSession(dappSession: DappSession): Completable

    /**
     * Update Dapp Session - update db record with current wallet connection
     * @param peerId - id of socket_client connection
     * @param address - address of account which was specified
     * @param chainId - chainId of account which was specified
     * @param accountName - name (with index) of account which was specified
     * @return Completable
     */
    fun updateDappSession(peerId: String, address: String, chainId: Int, accountName: String): Completable
    fun deleteDappSession(peerId: String): Completable
    fun getSessions(): Single<List<DappSession>>
    fun dispose()
    fun getDappSessionById(peerId: String): Single<DappSession>
    fun approveRequest(peerId: String, privateKey: String)
    fun rejectRequest(peerId: String)
    fun approveTransactionRequest(peerId: String, message: String)
    fun removeDeadSessions()
    fun killAllAccountSessions(address: String, chainId: Int): Completable
}