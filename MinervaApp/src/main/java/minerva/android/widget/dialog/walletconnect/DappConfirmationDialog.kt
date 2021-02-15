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


    fun setNotDefinedNetwork() {
        with(networkHeader.network) {
            background = context.getDrawable(R.drawable.network_not_defined_background)
            setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(context, R.drawable.ic_help),
                null,
                null,
                null
            )
            compoundDrawablePadding = resources.getDimension(R.dimen.margin_small).toInt()
        }
    }

    fun setNotDefinedNetworkWarning() = with(binding) {
        showWaring()
        setNotDefinedNetwork()
    }

    fun setWrongNetworkMessage(message: String) = with(binding) {
        warning.text = message
        showWaring()
    }

    private fun DappConfirmationDialogBinding.showWaring() {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
    }
}