package minerva.android.widget.dialog.walletconnect

import android.content.Context
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.invisible
import minerva.android.extension.setTextWithArgs
import minerva.android.extension.visible
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

    fun setNotDefinedNetworkWarning() = with(binding) {
        setNetworkHeader(R.drawable.network_not_defined_background)
        showWaring()
    }

    fun setWrongNetworkWarning(networkName: String) = with(binding) {
        with(networkHeader.network) {
            background = context.getDrawable(R.drawable.warning_background)
            setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
        }
        warringIcon.setImageResource(R.drawable.ic_warning)
        warning.setTextWithArgs(R.string.wrong_network_warning_message, networkName)
        warning.setTextColor(ContextCompat.getColor(context, R.color.warningMessageOrange))
        showWaring()
    }

    fun setWrongNetworkMessage(networkName: String) = with(binding) {
        setNetworkHeader(R.drawable.wrong_network_background)
        warning.setTextWithArgs(R.string.wrong_network_message, networkName)
        binding.confirmationButtons.confirm.isEnabled = false
        showWaring()
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}