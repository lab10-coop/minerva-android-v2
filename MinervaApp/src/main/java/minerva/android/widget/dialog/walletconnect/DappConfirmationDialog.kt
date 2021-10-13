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
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.OneElement
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.DynamicWidthSpinner

class DappConfirmationDialog(context: Context, approve: () -> Unit, deny: () -> Unit, private val onAddAccountClick: (Int) -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappConfirmationDialogBinding = DappConfirmationDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
        binding.confirmationButtons.confirm.text = context.getString(R.string.Connect)
        binding.confirmationView.hideRequestedData()
    }

    fun setView(meta: WalletConnectPeerMeta, networkName: String) = with(binding) {
        setupHeader(meta.name, networkName, getIcon(meta))
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
                prepareSpinner(R.drawable.rounded_background_purple_frame, defaultPosition) { position, view ->
                    onAccountSelected(accountAdapter.getItem(position))
                    accountAdapter.selectedItemWidth = view?.width
                }
            }
        }

    private fun isAccountSpinnerVisible(listSize: Int): Boolean = listSize > Int.OneElement && networkHeader.addAccount.isGone

    fun setNotDefinedNetworkWarning(availableNetworks: List<NetworkDataSpinnerItem>, onNetworkSelected: (Int) -> Unit) =
        with(binding) {
            setNetworkHeader()
            showWaring()
            networkHeader.network.gone()
            val networkAdapter = DappNetworksSpinnerAdapter(
                context,
                R.layout.spinner_network_wallet_connect,
                availableNetworks
            ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }
            updateNotDefinedNetworkWarning(networkAdapter.getItem(Int.FirstIndex))
            networkHeader.networkSpinner.apply {
                visible()
                addOnGlobalLayoutListener() {
                    networkAdapter.selectedItemWidth = networkHeader.accountSpinner.width
                }
                adapter = networkAdapter
                prepareSpinner(R.drawable.warning_background, Int.FirstIndex) { position, view ->
                    val selectedItem = networkAdapter.getItem(position)
                    networkAdapter.selectedItemWidth = view?.width
                    updateNotDefinedNetworkWarning(selectedItem)
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

    private fun updateNotDefinedNetworkWarning(item: NetworkDataSpinnerItem) = with(networkHeader) {
        val warningRes =
            if (item.isAccountAvailable) R.string.not_defined_warning_message else R.string.not_defined_warning_ethereum_message
        accountSpinner.visibleOrGone(item.isAccountAvailable)
        addAccount.apply {
            visibleOrGone(!item.isAccountAvailable)
            setupAddAccountListener(item.chainId)
        }
        setupWarning(warningRes)
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

    private fun setupWarning(warningRes: Int, networkName: String? = null) = with(binding) {
        warringIcon.setImageResource(R.drawable.ic_warning)
        networkName?.let { warning.setTextWithArgs(warningRes, networkName) }.orElse { warning.setText(warningRes) }
        warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}