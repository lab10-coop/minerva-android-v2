package minerva.android.accounts.walletconnect

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue
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
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequestV2
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
    abstract fun handleSessionRequestV2(sessionRequest: OnSessionRequestV2)
    abstract fun closeScanner(isMobileWalletConnect: Boolean = false)
    abstract fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType)

    protected open val selectedChainId
        get() = when {
            requestedNetwork.chainId != Int.InvalidId -> requestedNetwork.chainId
            account.chainId != Int.InvalidValue -> account.chainId
            else -> baseNetwork
        }

    var requestedNetwork: BaseNetworkData = BaseNetworkData(Int.InvalidId, String.Empty)
    internal var topic: Topic = Topic()
    private var handshakeId: Long = 0L
    internal var currentSession: WalletConnectSession = WalletConnectSession()

    private val baseNetwork get() = if (accountManager.areMainNetworksEnabled) ChainId.ETH_MAIN else ChainId.ETH_GOR

    val availableNetworks: List<NetworkDataSpinnerItem> get() = prepareAvailableNetworks()

    val availableAccounts: List<Account> get() = accountManager.getAllActiveAccounts(selectedChainId)

    val availableAddresses: List<String> get() = accountManager.getAllAccounts()
        .filter { account -> account.shouldShow }
        .map { account -> account.address }
        .distinct()

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
                            is OnSessionRequestV2 -> {
                                // todo: do topic and handshakeId need to be set here?
                                handleSessionRequestV2(status)
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

    fun approveSession(meta: WalletConnectPeerMeta, isMobileWalletConnect: Boolean) {
        if (account.id == Int.InvalidId) {
            return
        }
        launchDisposable {
            walletConnectRepository.approveSession(
                listOf(account.address),
                account.chainId,
                topic.peerId,
                getDapp(meta, account.chainId, account, isMobileWalletConnect)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        //set default value for showing empty list of network in future
                        requestedNetwork = BaseNetworkData(Int.InvalidId, String.Empty)
                        closeScanner(isMobileWalletConnect)
                    },
                    onError = { setLiveDataError(it) }
                )
        }
    }

    fun approveSessionV2(proposerPublicKey: String, namespace: WalletConnectSessionNamespace, isMobileWalletConnect: Boolean) {
        launchDisposable {
            walletConnectRepository.approveSessionV2(proposerPublicKey, namespace)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        //set default value for showing empty list of network in future
                        requestedNetwork = BaseNetworkData(Int.InvalidId, String.Empty)
                        closeScanner(isMobileWalletConnect)
                    },
                    onError = { setLiveDataError(it) }
                )
        }
    }

    /**
     * Update Session - update account for current wallet api connection
     * @param connectionPeerId - id of socket client connection
     */
    fun updateSession(connectionPeerId: String) {
        if (account.id != Int.InvalidId) {
            launchDisposable {
                walletConnectRepository.updateSession(
                    connectionPeerId,
                    account.address,
                    account.chainId,
                    account.name,
                    account.network.name
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { setLiveDataError(it) }
                    )
            }
        }
    }

    private fun getDapp(meta: WalletConnectPeerMeta, chainId: Int, account: Account, isMobileWalletConnect: Boolean) = DappSessionV1(
        account.address,
        currentSession.topic,
        currentSession.version,
        currentSession.bridge.toString(), // todo: fix this, that's not a real solution
        currentSession.key,
        meta.name,
        getIcon(meta.icons),
        topic.peerId,
        topic.remotePeerId,
        requestedNetwork.name,
        account.name,
        chainId,
        handshakeId,
        isMobileWalletConnect
    )

    private fun getIcon(icons: List<String>) =
        if (icons.isEmpty()) String.Empty
        else icons[Int.FirstIndex]


    open fun rejectSession(isMobileWalletConnect: Boolean = false) {
        walletConnectRepository.rejectSession(topic.peerId)
    }

    open fun rejectSessionV2(proposerPublicKey: String, reason: String, isMobileWalletConnect: Boolean = false) {
        walletConnectRepository.rejectSessionV2(proposerPublicKey, reason)
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
        NetworkManager.networks.find { network -> network.chainId == chainId && accountManager.areMainNetworksEnabled == !network.testNet && network.isActive} == null

    fun fetchUnsupportedNetworkName(chainId: Int) {
        launchDisposable {
            unsupportedNetworkRepository.getNetworkName(chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { chainName ->
                        updateWCState(BaseNetworkData(chainId, chainName), WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING)
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