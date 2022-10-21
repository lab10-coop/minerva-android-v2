package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import minerva.android.R
import minerva.android.accounts.walletconnect.DappAccountsSpinnerAdapter
import minerva.android.accounts.walletconnect.DappNetworksSpinnerAdapter
import minerva.android.accounts.walletconnect.NetworkDataSpinnerItem
import minerva.android.accounts.walletconnect.WalletConnectAlertType
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.*
import minerva.android.kotlinUtils.function.orElse
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

    private fun setNetworkHeader() {
        with(networkHeader.network) {
            background = ContextCompat.getDrawable(context, R.drawable.network_not_defined_background)
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    fun setupAccountSpinner(selectedAccountId: Int, availableAccounts: List<Account>, onAccountSelected: (Account) -> Unit) =
        with(binding) {
            val accountAdapter = DappAccountsSpinnerAdapter(
                context,
                R.layout.spinner_network_wallet_connect,
                availableAccounts
            ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }
            networkHeader.accountSpinner.apply {
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

    private fun isAccountSpinnerVisible(listSize: Int): Boolean = listSize > Int.EmptyResource && networkHeader.addAccount.isGone

    fun setNotDefinedNetworkWarning(
        availableNetworks: List<NetworkDataSpinnerItem>,
        dialogType: WalletConnectAlertType,
        currentDAppSessionChainId: Int = Int.InvalidId, //current chainId connection (or -1 if connection wasn't installed)
        onNetworkSelected: (Int) -> Unit)
    = with(binding) {
        setNetworkHeader()
        showWaring()
        networkHeader.network.gone()
        val networkAdapter = DappNetworksSpinnerAdapter(
            context,
            R.layout.spinner_network_wallet_connect,
            availableNetworks
        ).apply {
            setDropDownViewResource(R.layout.spinner_network_wallet_connect)
        }
        //get current DApp session (item) index from availableNetworks (for set this network(account) like selected in spinner)
        val networkItemIndex: Int =  if (Int.InvalidId == currentDAppSessionChainId) {
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
        updateNotDefinedNetworkWarning(networkAdapter.getItem(networkItemIndex), dialogType)
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
            prepareSpinner(R.drawable.warning_background, networkItemIndex) { position, view ->
                val selectedItem = networkAdapter.getItem(position)
                updateNotDefinedNetworkWarning(selectedItem, dialogType)
                if (selectedItem.isAccountAvailable) {
                    onNetworkSelected(selectedItem.chainId)
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

    private fun updateNotDefinedNetworkWarning(item: NetworkDataSpinnerItem, dialogType: WalletConnectAlertType) = with(networkHeader) {
        val warningRes =
            if (item.isAccountAvailable) {
                //set correct description for message
                if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType)
                    R.string.change_account_not_defined_warning_message
                else
                    R.string.not_defined_warning_message
            } else R.string.not_defined_warning_ethereum_message
        accountSpinner.visibleOrGone(item.isAccountAvailable)
        addAccount.apply {
            visibleOrGone(!item.isAccountAvailable)
            setupAddAccountListener(item.chainId)
        }
        setupWarning(
            warningRes,
            null,
            WalletConnectAlertType.CHANGE_ACCOUNT != dialogType
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


    private fun getWarningText(network: BaseNetworkData, context: Context) =
        if (network.name == String.Empty) context.getString(
            R.string.unsupported_network_id_message,
            *arrayOf(network.chainId)
        ) else context.getString(
            R.string.unsupported_network_name_message,
            *arrayOf(network.name))


    fun setUnsupportedNetworkMessage(network: BaseNetworkData) = with(binding) {
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

        warning.text = getWarningText(network, context)
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
            accountSpinner.gone()
            networkSpinner.gone()
        }
    }

    private fun setupWarning(warningRes: Int, networkName: String? = null, warningColor: Boolean = true/*color for message*/) = with(binding) {
        warringIcon.setImageResource(R.drawable.ic_warning)
        networkName?.let { warning.setTextWithArgs(warningRes, networkName) }.orElse { warning.setText(warningRes) }
        if (warningColor) {
            warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        } else {
            warning.setTextColor(ContextCompat.getColor(context, R.color.darkGray70))
        }
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}