package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.DefaultRequest
import minerva.android.accounts.walletconnect.OnDisconnected
import minerva.android.accounts.walletconnect.OnEthSignRequest
import minerva.android.accounts.walletconnect.WalletConnectState
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.WalletConnectSession
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnEthSign
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectStatus
import timber.log.Timber

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    lateinit var currentDappSession: DappSession

    private val _walletConnectStatus = MutableLiveData<WalletConnectState>()
    val walletConnectStatus: LiveData<WalletConnectState> get() = _walletConnectStatus


    fun dispose() {
        walletConnectRepository.dispose()
    }

    init {
        launchDisposable {
            walletConnectRepository.getSessions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { reconnect(it) },
                    onError = { Timber.e(it) }
                )
        }
    }

    private fun reconnect(dapps: List<DappSession>) {
        dapps.forEach { session ->
            with(session) {
                walletConnectRepository.connect(WalletConnectSession(topic, version, bridge, key), peerId, remotePeerId)
            }
        }
        subscribeToWalletConnectEvents()
    }

    private fun subscribeToWalletConnectEvents() {
        launchDisposable {
            walletConnectRepository.connectionStatusFlowable
                .flatMapSingle { mapRequests(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _walletConnectStatus.value = it },
                    onError = { Timber.e(it) }
                )
        }
    }

    private fun mapRequests(it: WalletConnectStatus) = when (it) {
        is OnEthSign -> {
            Timber.tag("kobe").d("mapping onEthSign")
            walletConnectRepository.getDappSessionById(it.peerId)
                .map { session ->
                    Timber.tag("kobe").d("session ${session.address}")
                    currentDappSession = session
                    OnEthSignRequest(it.message, session)
                }
        }
        is OnDisconnect -> Single.just(OnDisconnected)
        else -> Single.just(DefaultRequest)
    }

    fun acceptRequest() {
        val account = transactionRepository.getAccountByAddress(currentDappSession.address)

        Timber.tag("kobe").d("account address ${account?.address}")
        Timber.tag("kobe").d("account priv key ${account?.privateKey}")

        val privateMasterKey = transactionRepository.masterSeed.privateKey
        Timber.tag("kobe").d("master priv key $privateMasterKey")

        account?.privateKey?.let {
            walletConnectRepository.approveRequest(
                currentDappSession.peerId,
                privateMasterKey,
                transactionRepository.getMnemonic()
            )
        }
    }

    fun rejectRequest() {
        walletConnectRepository.rejectRequest(currentDappSession.peerId)
    }
}