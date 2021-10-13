package minerva.android.accounts.walletconnect

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.*
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnFailure
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

abstract class BaseWalletConnectScannerViewModel(
    private val accountManager: AccountManager,
    private val walletConnectRepository: WalletConnectRepository,
    private val logger: Logger,
    private val walletActionsRepository: WalletActionsRepository,
    private val unsupportedNetworkRepository: UnsupportedNetworkRepository
) : BaseViewModel() {

    abstract var account: Account

    abstract fun hideProgress()
    abstract fun setLiveDataOnDisconnected(sessionName: String)
    abstract fun setLiveDataOnConnectionError(error: Throwable, sessionName: String)
    abstract fun setLiveDataError(error: Throwable)
    abstract fun handleSessionRequest(sessionRequest: OnSessionRequest)
    abstract fun closeScanner()
    abstract fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType)

    protected open val selectedChainId
        get() = when {
            requestedNetwork.chainId != Int.InvalidId -> requestedNetwork.chainId
            account.chainId != Int.InvalidValue -> account.chainId
            else -> baseNetwork
        }

    var requestedNetwork: BaseNetworkData = BaseNetworkData(Int.InvalidId, String.Empty)
    internal lateinit var topic: Topic
    private var handshakeId: Long = 0L
    internal lateinit var currentSession: WalletConnectSession

    private val baseNetwork get() = if (accountManager.areMainNetworksEnabled) ChainId.ETH_MAIN else ChainId.ETH_GOR

    val availableNetworks: List<NetworkDataSpinnerItem> get() = prepareAvailableNetworks()

    val availableAccounts: List<Account> get() = accountManager.getAllActiveAccounts(selectedChainId)

    private fun prepareAvailableNetworks(): List<NetworkDataSpinnerItem> =
        mutableListOf<NetworkDataSpinnerItem>().apply {
            val availableAccountList = accountManager.getFirstActiveAccountForAllNetworks()
            add(
                Int.FirstIndex,
                NetworkDataSpinnerItem(
                    getNetworkName(baseNetwork),
                    baseNetwork,
                    availableAccountList.find { account -> account.chainId == baseNetwork } != null))
            addAll(availableAccountList.filter { account -> account.chainId != baseNetwork }
                .map { account -> NetworkDataSpinnerItem(account.network.name, account.chainId) }
            )
        }

    fun subscribeToWCConnectionStatusFlowable() {
        launchDisposable {
            walletConnectRepository.connectionStatusFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { status ->
                        hideProgress()
                        when (status) {
                            is OnSessionRequest -> {
                                topic = status.topic
                                handshakeId = status.handshakeId
                                handleSessionRequest(status)
                            }
                            is OnDisconnect -> setLiveDataOnDisconnected(status.sessionName)
                            is OnFailure -> {
                                logger.logToFirebase("OnWalletConnectConnectionError: ${status.error}")
                                setLiveDataOnConnectionError(status.error, status.sessionName)
                            }
                            else -> DefaultRequest
                        }
                    },
                    onError = { error ->
                        logger.logToFirebase("WalletConnect statuses error: $error")
                        setLiveDataError(error)
                    }
                )
        }
    }

    fun setAccountForSelectedNetwork(chainId: Int) {
        accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
        }
    }

    fun setNewAccount(newAccount: Account) {
        account = newAccount
    }

    fun approveSession(meta: WalletConnectPeerMeta) {
        if (account.id != Int.InvalidId) {
            launchDisposable {
                walletConnectRepository.approveSession(
                    listOf(account.address),
                    account.chainId,
                    topic.peerId,
                    getDapp(meta, account.chainId, account)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onComplete = { closeScanner() },
                        onError = { setLiveDataError(it) }
                    )
            }
        }
    }

    private fun getDapp(meta: WalletConnectPeerMeta, chainId: Int, account: Account) = DappSession(
        account.address,
        currentSession.topic,
        currentSession.version,
        currentSession.bridge,
        currentSession.key,
        meta.name,
        getIcon(meta.icons),
        topic.peerId,
        topic.remotePeerId,
        requestedNetwork.name,
        account.name,
        chainId,
        handshakeId
    )

    private fun getIcon(icons: List<String>) =
        if (icons.isEmpty()) String.Empty
        else icons[Int.FirstIndex]


    open fun rejectSession() {
        walletConnectRepository.rejectSession(topic.peerId)
    }

    fun addAccount(chainId: Int, dialogType: WalletConnectAlertType) {
        launchDisposable {
            accountManager.createOrUnhideAccount(NetworkManager.getNetwork(chainId))
                .doOnSuccess { name ->
                    setAccountForSelectedNetwork(chainId)
                    val newDialogType = if (dialogType == WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR) {
                        WalletConnectAlertType.NO_ALERT
                    } else dialogType
                    updateWCState(requestedNetwork, newDialogType)
                    walletActionsRepository.saveWalletActions(listOf(getWalletAddAction(name)))
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error ->
                        Timber.e(error)
                        setLiveDataError(error)
                    }
                )
        }
    }

    protected fun getNetworkName(chainId: Int): String =
        NetworkManager.networks.find { network -> network.chainId == chainId }?.name.orElse { String.Empty }

    protected fun isNetworkNotSupported(chainId: Int): Boolean =
        NetworkManager.networks.find { network -> network.chainId == chainId } == null

    fun fetchUnsupportedNetworkName(chainId: Int) {
        launchDisposable {
            unsupportedNetworkRepository.getNetworkName(chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { chainName ->
                        updateWCState(BaseNetworkData(chainId, chainName), WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING)
                    },
                    onError = {
                        setLiveDataError(it)
                    }
                )
        }
    }

    private fun getWalletAddAction(name: String) =
        WalletAction(
            WalletActionType.ACCOUNT,
            WalletActionStatus.ADDED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, name))
        )

}