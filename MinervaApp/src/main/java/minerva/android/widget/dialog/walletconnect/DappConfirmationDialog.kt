package minerva.android.widget.dialog.walletconnect

import android.content.Context
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta

class DappConfirmationDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappConfirmationDialogBinding = DappConfirmationDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
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

    fun setWrongNetworkMessage(message: String) = with(binding) {
        setNetworkHeader(R.drawable.wrong_network_background)
        warning.text = message
        binding.confirmationButtons.connect.isEnabled = false
        showWaring()
    }

    private fun showWaring() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}