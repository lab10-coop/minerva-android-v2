package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.accounts.walletconnect.DappAccountsSpinnerAdapter
import minerva.android.accounts.walletconnect.DappNetworksSpinnerAdapter
import minerva.android.accounts.walletconnect.NetworkDataSpinnerItem
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.DynamicWidthSpinner

class DappConfirmationDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
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

    private fun setNetworkHeader(backgroundResId: Int) {
        with(networkHeader.network) {
            background = ContextCompat.getDrawable(context, backgroundResId)
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
                addOnGlobalLayoutListener() {
                    accountAdapter.selectedItemWidth = networkHeader.accountSpinner.width
                }
                adapter = accountAdapter
                val defaultPosition = availableAccounts.indexOfFirst { account -> account.id == selectedAccountId }
                prepareSpinner(R.drawable.rounded_background_purple_frame, defaultPosition) { position, view ->
                    onAccountSelected(accountAdapter.getItem(position))
                    accountAdapter.selectedItemWidth = view?.width
                }
            }
        }

    fun setNotDefinedNetworkWarning(availableNetworks: List<NetworkDataSpinnerItem>, onNetworkSelected: (Int) -> Unit) =
        with(binding) {
            setNetworkHeader(R.drawable.network_not_defined_background)
            showWaring()
            networkHeader.network.gone()
            val networkAdapter = DappNetworksSpinnerAdapter(
                context,
                R.layout.spinner_network_wallet_connect,
                availableNetworks
            ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }
            updateNotDefinedNetworkWarning(networkAdapter.getItem(Int.FirstIndex))
            networkHeader.networkSpinner.apply {
                addOnGlobalLayoutListener() {
                    networkAdapter.selectedItemWidth = networkHeader.accountSpinner.width
                }
                adapter = networkAdapter
                prepareSpinner(R.drawable.warning_background, Int.FirstIndex) { position, view ->
                    val selectedItem = networkAdapter.getItem(position)
                    if (selectedItem.isAccountAvailable) {
                        onNetworkSelected(selectedItem.chainId)
                    }
                    networkAdapter.selectedItemWidth = view?.width
                    updateNotDefinedNetworkWarning(selectedItem)
                }
            }
        }

    private fun DynamicWidthSpinner.prepareSpinner(backgroundResId: Int, selectionIndex: Int, onClick: (Int, View?) -> Unit) {
        visible()
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

    private fun updateNotDefinedNetworkWarning(item: NetworkDataSpinnerItem) = with(binding) {
        val warningRes =
            if (item.isAccountAvailable) R.string.not_defined_warning_message else R.string.not_defined_warning_ethereum_message
        networkHeader.accountSpinner.visibleOrGone(item.isAccountAvailable)
        warning.setText(warningRes)
        warringIcon.setImageResource(R.drawable.ic_warning)
        warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        confirmationButtons.confirm.isEnabled = item.isAccountAvailable
    }

    fun setUnsupportedNetworkMessage(networkId: String) = with(binding) {
        networkHeader.network.apply {
            setTextWithArgs(R.string.chain_id, networkId)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_alert_small, NO_ICON, NO_ICON, NO_ICON)
            setBackgroundResource(R.drawable.error_background)
            setTextColor(ContextCompat.getColor(context, R.color.alertRed))
        }
        warning.setTextWithArgs(R.string.unsupported_network_message, networkId)
        binding.confirmationButtons.confirm.isEnabled = false
        showWaring()
    }

    fun setNoAvailableAccountMessage(networkName: String) = with(binding) {
        warringIcon.setImageResource(R.drawable.ic_warning)
        warning.setTextWithArgs(R.string.missing_account_message, networkName)
        warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        binding.confirmationButtons.confirm.isEnabled = false
        showWaring()
    }

    fun setChangeAccountMessage(networkName: String) = with(binding) {
        warringIcon.setImageResource(R.drawable.ic_warning)
        warning.setTextWithArgs(R.string.change_account_warning, networkName)
        warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        showWaring()
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}