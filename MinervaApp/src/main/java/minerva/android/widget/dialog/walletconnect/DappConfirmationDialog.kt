package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import minerva.android.R
import minerva.android.accounts.walletconnect.*
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.*
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.MainActivity
import minerva.android.main.handler.replaceFragment
import minerva.android.settings.advanced.AdvancedFragment
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.DynamicWidthSpinner
import minerva.android.widget.dialog.models.ViewDetails

class DappConfirmationDialog(context: Context, approve: () -> Unit, deny: () -> Unit, private val onAddAccountClick: (Int) -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappConfirmationDialogBinding = DappConfirmationDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)
    //current DApp session wallet connection
    var dAppSessionMeta: WalletConnectPeerMeta? = null

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
        binding.confirmationView.hideRequestedData()
    }

    /**
     * Change Clickable Confirm Button State - check and change confirm button state for prevent extra db records
     * @param address - address of specified account
     * @param address - chain id of specified account
     */
    fun changeClickableConfirmButtonState(address: String, chainId: Int) {
        dAppSessionMeta?.let { session ->
            //compare specified meta data and db stored data (current connection)
            val isActive: Boolean = !(address == session.address && chainId == session.chainId)
            binding.confirmationButtons.confirm.apply {
                isClickable = isActive
                isEnabled = isActive
            }
        }
    }

    /**
     * Set View - prepare global variables and set some state for popap dialog
     * @param meta - set current wallet connection DApp session (from db)
     * @param viewDetails - popap dialog view details
     */
    fun setView(
        meta: WalletConnectPeerMeta,
        viewDetails: ViewDetails)
    = with(binding) {
        //set current wallet connection dapp session
        dAppSessionMeta = meta
        setupHeader(meta.name, viewDetails.networkName, getIcon(meta))
        binding.apply {
            confirmationButtons.confirm.text = viewDetails.confirmButtonName
            connectionName.text = viewDetails.connectionName
        }
    }

    private fun DappConfirmationDialogBinding.getIcon(meta: WalletConnectPeerMeta): Any =
        if (meta.icons.isEmpty()) {
            confirmationView.setDefaultIcon()
            R.drawable.ic_services
        } else {
            confirmationView.setIcon(meta.icons[FIRST_ICON])
            meta.icons[FIRST_ICON]
        }

    /**
     * Set Network Header - set layout details
     * @param - instance of minerva.android.accounts.walletconnect.WalletConnectAlertType
     */
    private fun setNetworkHeader(dialogType: WalletConnectAlertType) {
        networkHeader.apply {
            network.apply {
                if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_background_gray_frame)
                    setTextColor(ContextCompat.getColor(context, R.color.dappStatusColorGray))
                } else {
                    background = ContextCompat.getDrawable(context, R.drawable.network_not_defined_background)
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                }
            }
            addAccount.apply {
                if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_background_gray_frame)
                    setTextColor(ContextCompat.getColor(context, R.color.dappStatusColorGray))
                }
            }
            arrowSeparator.apply {
                visibility = if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    fun setupAccountSpinner(selectedAccountId: Int, availableAccounts: List<Account>, dialogType: WalletConnectAlertType, onAccountSelected: (Account) -> Unit) {
        networkHeader.accountSpinner.apply {
            if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                gone()
            } else {
                val accountAdapter = DappAccountsSpinnerAdapter(
                    context,
                    R.layout.spinner_network_wallet_connect,
                    availableAccounts
                ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }

                visibleOrGone(isAccountSpinnerVisible(availableAccounts.size))
                addOnGlobalLayoutListener {
                    accountAdapter.selectedItemWidth = networkHeader.accountSpinner.width
                }
                adapter = accountAdapter
                val defaultPosition =
                    if (selectedAccountId != Int.InvalidId) {
                        availableAccounts.indexOfFirst { account -> account.id == selectedAccountId }
                    } else Int.FirstIndex
                //change state of confirm button for prevent the same db records
                if (availableAccounts.isNotEmpty()) {
                    changeClickableConfirmButtonState(availableAccounts[defaultPosition].address, availableAccounts[defaultPosition].chainId)
                }
                prepareSpinner(R.drawable.rounded_background_purple_frame, defaultPosition) { position, view ->
                    onAccountSelected(accountAdapter.getItem(position))
                    accountAdapter.selectedItemWidth = view?.width
                }
            }
        }
    }

    private fun isAccountSpinnerVisible(listSize: Int): Boolean = listSize > Int.EmptyResource && networkHeader.addAccount.isGone

    fun setNotDefinedNetworkWarning(
        availableNetworks: List<NetworkDataSpinnerItem>,
        dialogType: WalletConnectAlertType,
        currentDAppSessionChainId: Int = Int.InvalidId, //current chainId connection (or -1 if connection wasn't installed)
        network: BaseNetworkData,
        onNetworkSelected: (Int) -> Unit)
    = with(binding) {
        setNetworkHeader(dialogType)
        showWaring()
        val networkAdapter = DappNetworksSpinnerAdapter(
            context,
            R.layout.spinner_network_wallet_connect,
            availableNetworks
        ).apply {
            setDropDownViewResource(R.layout.spinner_network_wallet_connect)
        }
        //get current DApp session (item) index from availableNetworks (for set this network(account) like selected in spinner)
        val networkItemIndex: Int = if (Int.InvalidId == currentDAppSessionChainId || Int.ONE == availableNetworks.size) {
            Int.FirstIndex
        } else {
            var indexByChainId: Int = Int.FirstIndex //DApp session default value
            //trying to find index in availableNetworks by chainId
            availableNetworks.forEachIndexed { index, networkDataSpinnerItem ->
                if (currentDAppSessionChainId == networkDataSpinnerItem.chainId) {
                    indexByChainId = index
                }
            }
            indexByChainId
        }
        updateNotDefinedNetworkWarning(networkAdapter.getItem(networkItemIndex), dialogType, network.name)
        if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
            networkHeader.network.text = availableNetworks.first().networkName
        } else {
            networkHeader.network.gone()
            networkHeader.networkSpinner.apply {
                visible()
                addOnGlobalLayoutListener() {
                    //the longest name length of network list
                    var longestNameLength = 0
                    val BASE_NAME_SIZE = 200
                    val SYMBOL_SIZE = 14
                    availableNetworks.forEach { network ->
                        if (network.networkName.length > longestNameLength) longestNameLength = network.networkName.length
                    }
                    //set correct view width (for showing full name of network in dropdown menu)
                    networkAdapter.selectedItemWidth = BASE_NAME_SIZE + (longestNameLength * SYMBOL_SIZE)
                }
                adapter = networkAdapter
                prepareSpinner(R.drawable.warning_background, networkItemIndex) { position, _ ->
                    val selectedItem = networkAdapter.getItem(position)
                    updateNotDefinedNetworkWarning(selectedItem, dialogType)
                    if (selectedItem.isAccountAvailable) {
                        onNetworkSelected(selectedItem.chainId)
                    }
                }
            }
        }
    }

    private fun DynamicWidthSpinner.prepareSpinner(backgroundResId: Int, selectionIndex: Int, onClick: (Int, View?) -> Unit) {
        setBackgroundResource(backgroundResId)
        setPopupBackgroundResource(R.drawable.rounded_small_white_background)
        setSelection(selectionIndex, false)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onClick(position, view)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun updateNotDefinedNetworkWarning(item: NetworkDataSpinnerItem, dialogType: WalletConnectAlertType, networkName: String = "") = with(networkHeader) {
        val warningRes: Int =
            if (item.isAccountAvailable) {
                //set correct description for message
                if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType)
                    R.string.change_account_not_defined_warning_message
                else if (WalletConnectAlertType.CHANGE_NETWORK == dialogType)
                    R.string.change_network_warning_message
                else
                    R.string.not_defined_warning_message
            } else R.string.not_defined_warning_ethereum_message
        accountSpinner.visibleOrGone(item.isAccountAvailable)
        addAccount.apply {
            if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                visibleOrGone(true)
                text = networkName
            } else {
                visibleOrGone(!item.isAccountAvailable)
                setupAddAccountListener(item.chainId)
            }
        }
        setupWarning(
            warningRes,
            null,
            dialogType
        )
        binding.confirmationButtons.confirm.isEnabled = item.isAccountAvailable
    }

    private fun setupAddAccountListener(chainId: Int) = with(networkHeader.addAccount) {
        setOnClickListener {
            onAddAccountClick(chainId)
            gone()
        }
    }

    private fun getHeaderText(network: BaseNetworkData, context: Context) = if (network.name == String.Empty) context.getString(
            R.string.chain_id,
            *arrayOf(network.chainId)
        ) else context.getString(
            R.string.chain_name,
            *arrayOf(network.name))


    private fun getWarningText(network: BaseNetworkData, context: Context, kinfOfNetwork: KindOfNetwork = KindOfNetwork.EQUAL): String  = when (kinfOfNetwork) {
        KindOfNetwork.EQUAL -> {
            if (network.name == String.Empty) context.getString(
                R.string.unsupported_network_id_message,
                *arrayOf(network.chainId)
            ) else context.getString(
                R.string.unsupported_network_name_message,
                *arrayOf(network.name))
        }
        KindOfNetwork.MAIN -> context
            .getString(R.string.switch_network,
                *arrayOf(KindOfNetwork.TEST.name.toLowerCase()))
        KindOfNetwork.TEST -> context
            .getString(R.string.switch_network,
                *arrayOf(KindOfNetwork.MAIN.name.toLowerCase()))
    }


    fun setUnsupportedNetworkMessage(network: BaseNetworkData, requestChainId: Int = Int.InvalidId) = with(binding) {
        networkHeader.network.apply {
            networkHeader.network.text = getHeaderText(network, context)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_alert_small,
                NO_ICON,
                NO_ICON,
                NO_ICON
            )
            setBackgroundResource(R.drawable.error_background)
            setTextColor(ContextCompat.getColor(context, R.color.alertRed))
        }

        warning.text = if (requestChainId == Int.InvalidId)
            getWarningText(network, context)
        else {
            var warningText: String = String.Empty

            //using "try/catch" for prevent exception while getting network from api(unsupported chain)
            try {
                val requestNetwork = NetworkManager.getNetwork(requestChainId)
                val responseNetwork = NetworkManager.getNetwork(network.chainId)//network from api (DApp) response

                //when network type the same (main or test) get default warning message; when isn't the same - regarding change network type by type of api response
                warningText = if (requestNetwork.testNet == responseNetwork.testNet) {
                    getWarningText(network, context)
                } else {
                    if (requestNetwork.testNet) {
                        getWarningText(network, context, KindOfNetwork.TEST)
                    } else {
                        getWarningText(network, context, KindOfNetwork.MAIN)
                    }
                }
            } catch (e: Exception) { }

            //if we get error from api get default warning text
            if (warningText == String.Empty)
                getWarningText(network, context)
            else {
                warningText
            }
        }
        confirmationButtons.confirm.isEnabled = false
        showWaring()
    }

    fun setNoAvailableAccountMessage(network: BaseNetworkData) = with(binding) {
        setupWarning(R.string.missing_account_message, network.name)
        confirmationButtons.confirm.isEnabled = false
        networkHeader.addAccount.visible()
        networkHeader.accountSpinner.gone()
        setupAddAccountListener(network.chainId)
        showWaring()
    }

    fun setChangeAccountMessage(networkName: String) = with(binding) {
        setupWarning(R.string.change_account_warning, networkName)
        showWaring()
    }

    fun setNoAlert() = with(binding) {
        warringIcon.gone()
        warning.gone()
        manual.visible()
        confirmationButtons.confirm.isEnabled = true
        networkHeader.apply {
            addAccount.gone()
            accountSpinner.visible()
            networkSpinner.gone()
        }
    }

    private fun setupWarning(
        warningRes: Int,
        networkName: String? = null,
        dialogType: WalletConnectAlertType? = null
    ) = with(binding) {
        warringIcon.setImageResource(R.drawable.ic_warning)
        networkName?.let {
            warning.setTextWithArgs(warningRes, networkName)
        }.orElse {
            if (WalletConnectAlertType.CHANGE_NETWORK == dialogType) {
                //create message with link for getting to "Settings" page (to "Advanced" tab)
                val message: String = context.resources.getString(warningRes)
                val wordForLink: String = "Settings"//word which would be clickable in warning message
                val wordForLinkStartFrom: Int = message.indexOf(wordForLink)//number of first letter
                val clickableSpan = object : ClickableSpan() {//create callback for link
                    override fun onClick(v: View) {
                        this@DappConfirmationDialog.cancel()//close dialog button
                        //get instance of MainActivity from cache
                        val mainActivityInstance = ((context as ContextThemeWrapper).baseContext as MainActivity)
                        //set "settings" tab (in bottom menu) to chosen state
                        mainActivityInstance.binding.bottomNavigation.menu.findItem(R.id.settings).isChecked = true
                        mainActivityInstance.replaceFragment(AdvancedFragment.newInstance(), R.string.advanced)//go to AdvancedFragment
                    }
                }
                val ss: SpannableString = SpannableString(message)//wrapper for clickable substring on w.message
                //set clickable range to w.message
                ss.setSpan(clickableSpan, wordForLinkStartFrom, wordForLinkStartFrom + wordForLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                warning.apply {//paste message (with link) to textView
                    text = ss
                    movementMethod = LinkMovementMethod.getInstance()//settings for link correct work
                }
            } else {
                warning.setText(warningRes)
            }
        }
        when (dialogType) {
            WalletConnectAlertType.CHANGE_ACCOUNT, WalletConnectAlertType.CHANGE_NETWORK ->
                warning.setTextColor(ContextCompat.getColor(context, R.color.darkGray70))
            else -> warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        }
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }

    /**
     * Network Type - enum for comparing network type
     *
     */
    enum class KindOfNetwork {
        MAIN, TEST, EQUAL;
    }
}