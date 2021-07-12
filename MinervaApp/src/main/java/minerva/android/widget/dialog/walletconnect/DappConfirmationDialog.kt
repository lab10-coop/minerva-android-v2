package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.accounts.walletconnect.DappNetworksSpinnerAdapter
import minerva.android.accounts.walletconnect.NetworkDataSpinnerItem
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.NO_ICON
import minerva.android.extension.invisible
import minerva.android.extension.setTextWithArgs
import minerva.android.extension.visible
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta

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
            background = context.getDrawable(backgroundResId)
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    fun setNotDefinedNetworkWarning(availableNetworks: List<NetworkDataSpinnerItem>, onNetworkSelected: (Int) -> Unit) =
        with(binding) {
            setNetworkHeader(R.drawable.network_not_defined_background)
            showWaring()
            networkHeader.network.invisible()
            val networkAdapter = DappNetworksSpinnerAdapter(
                context,
                R.layout.spinner_network_wallet_connect,
                availableNetworks
            ).apply { setDropDownViewResource(R.layout.spinner_network_wallet_connect) }
            updateNotDefinedNetworkWarning(networkAdapter.getItem(Int.FirstIndex))
            networkHeader.networkSpinner.apply {
                visible()
                setBackgroundResource(R.drawable.warning_background)
                adapter = networkAdapter
                setPopupBackgroundResource(R.drawable.rounded_white_background)
                setSelection(Int.FirstIndex, false)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedItem = networkAdapter.getItem(position)
                        if (selectedItem.isAccountAvailable) {
                            onNetworkSelected(selectedItem.chainId)
                        }
                        updateNotDefinedNetworkWarning(selectedItem)
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
                }
            }
        }

    private fun updateNotDefinedNetworkWarning(item: NetworkDataSpinnerItem) = with(binding) {
        val warningRes =
            if (item.isAccountAvailable) R.string.not_defined_warning_message else R.string.not_defined_warning_ethereum_message
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