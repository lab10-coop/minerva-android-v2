package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.*
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.AddressStatus
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.walletconnect.*
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
    abstract var address: String
    abstract fun hideProgress()
    abstract fun setLiveDataOnDisconnected(sessionName: String)
    abstract fun setLiveDataOnConnectionError(error: Throwable, sessionName: String)
    abstract fun setLiveDataError(error: Throwable)
    abstract fun handleSessionRequest(sessionRequest: OnSessionRequest)
    abstract fun handleSessionRequestV2(sessionRequest: OnSessionRequestV2)
    abstract fun closeScanner(isMobileWalletConnect: Boolean = false)
    abstract fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType)
    //property which specified that application must be closed (to background)
    private val _closeState: MutableLiveData<Boolean> = MutableLiveData(false)
    val closeState: LiveData<Boolean> get() = _closeState

    val areMainNetworksEnabled: Boolean
        get() = accountManager.areMainNetworksEnabled

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

    val networks: List<Network> = NetworkManager.networks
        .filter { it.isActive }
        .filter { !it.testNet == accountManager.areMainNetworksEnabled }

    val availableAccounts: List<Account> get() = accountManager.getAllActiveAccounts(selectedChainId)

    // using AddressWrapper because id and address are needed, AddressStatus doesn't matter here.
    val availableAddresses: List<AddressWrapper> get() = accountManager.getAllAccountsForSelectedNetworksType()
        .filter { account -> account.shouldShow }
        .map { account -> AddressWrapper(account.id, account.address, AddressStatus.ALREADY_IN_USE) }
        .distinct()
        .sortedBy { it.index }

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

                                if (null == status.type) {
                                    handleSessionRequest(status)
                                } else {
                                    //Int.ONE - CHANE NETWORK(through api) case
                                    if (Int.ONE == status.type && !accountManager.isChangeNetworkEnabled) {
                                        handleSessionRequest(status)
                                    } else {
                                        if (status.meta.isMobileWalletConnect) {
                                            toCloseState()//close application(to background)
                                        }
                                    }
                                }
                            }
                            is OnSessionRequestV2 -> {
                                // topic and handshakeId only need to be set for WC 1.0
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

    // for walletconnect 2.0
    fun setNewAddress(newAddress: String) {
        address = newAddress
    }

    fun approveSession(meta: WalletConnectPeerMeta) {
        if (account.id == Int.InvalidId) {
            return
        }
        launchDisposable {
            walletConnectRepository.approveSession(
                listOf(account.address),
                account.chainId,
                topic.peerId,
                getDapp(meta, account.chainId, account, meta.isMobileWalletConnect)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        //set default value for showing empty list of network in future
                        requestedNetwork = BaseNetworkData(Int.InvalidId, String.Empty)
                        closeScanner(meta.isMobileWalletConnect)
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
                        closeScanner(isMobileWalletConnect)//if true - close phone to background
                    },
                    onError = { setLiveDataError(it) }
                )
        }
    }

    /**
     * Update Session - method for updating specified dappSession (db record)
     * @param meta - new data for updating db
     */
    fun updateSession(meta: WalletConnectPeerMeta, newChainId: Int): Unit {
        if (account.id != Int.InvalidId) {
            launchDisposable {
                walletConnectRepository.updateSession(
                    connectionPeerId = meta.peerId,
                    accountAddress = account.address,
                    accountChainId = if (Int.InvalidId == newChainId) account.chainId else newChainId,
                    accountName = account.name,
                    networkName = account.network.name,
                    handshakeId = meta.handshakeId
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onComplete = {
                            //set default value for showing empty list of network in future
                            requestedNetwork = BaseNetworkData(Int.InvalidId, String.Empty)
                            closeScanner(meta.isMobileWalletConnect)//if true - close phone to background
                        },
                        onError = { setLiveDataError(it) }
                    )
            }
        }
    }

    // todo: only use this for WC 1.0, this need to be verified
    private fun getDapp(meta: WalletConnectPeerMeta, chainId: Int, account: Account, isMobileWalletConnect: Boolean) = DappSessionV1(
        account.address,
        currentSession.topic,
        currentSession.version,
        currentSession.bridge ?: String.Empty,
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


    /**
     * Reject Session - method for rejection(close) connection with api
     * @param isMobileWalletConnect - specified which type of work(connection) with api we using(mobile/desktop)
     * @param dialogType - in some cases we need to (just) skip just close dialog and don't "reject" session/connection
     */
    open fun rejectSession(isMobileWalletConnect: Boolean = false, dialogType: WalletConnectAlertType = WalletConnectAlertType.NO_ALERT) {
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

    /**
     * To Close State - set _closeState to "true" (move application to background if this isn't "scanner"(desktop) case)
     */
    private fun toCloseState() {
        _closeState.value = true
    }
}