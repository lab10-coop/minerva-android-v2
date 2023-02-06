package minerva.android.widget.dialog.walletconnect

import android.content.Context
import minerva.android.R
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogV2Binding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.extension.*
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.dialog.models.ViewDetailsV2

class DappConfirmationDialogV2(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappConfirmationDialogV2Binding = DappConfirmationDialogV2Binding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)
    //current DApp session wallet connection
    var dAppSessionMeta: WalletConnectPeerMeta? = null

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
        binding.confirmationView.hideRequestedData()
    }

    /**
     * Set View - prepare global variables and set some state for popup dialog
     * @param meta - set current wallet connection DApp session (from db)
     * @param viewDetails - popup dialog view details
     */
    fun setView(
        meta: WalletConnectPeerMeta,
        viewDetails: ViewDetailsV2
    )
    = with(binding) {
        //set current wallet connection dapp session
        dAppSessionMeta = meta
        // todo: localization
        setupHeader(meta.name, "Requested: " + viewDetails.networkNames.joinToString(" • "), getIcon(meta))
        binding.apply {
            confirmationButtons.confirm.text = viewDetails.confirmButtonName
            connectionName.text = viewDetails.connectionName
        }
    }

    // todo: check if this is correct
    fun setNoAlert() = with(binding) {
        warringIcon.gone()
        warning.gone()
        manual.visible()
        confirmationButtons.confirm.isEnabled = true
        networkHeader.apply {
            networkWarning.text = "Fully supported (x networks)" // todo: localize
            addAccount.gone()
            accountSpinner.visible()
            networkSpinner.gone()
        }
    }

    // todo: implement
    fun setUnsupportedNetworkWarning() = with(binding) {
        networkHeader.apply {
            networkWarning.text = "The request is not supported."
        }
        confirmationButtons.confirm.isEnabled = false
        // todo: localize
        manual.text = "If you would like to get this website supported, please engage with the Minerva team."
    }

    // todo: implement
    fun setOtherUnsupportedWarning() = with(binding) {
        networkHeader.apply {
            networkWarning.text = "Fully supported (x networks)" // todo: localize
        }
        confirmationButtons.confirm.isEnabled = false
        // todo: localize
        manual.text = "This website requests events or methods that are not supported. Please engage with the Minerva team."
    }

    private fun DappConfirmationDialogV2Binding.getIcon(meta: WalletConnectPeerMeta): Any =
        if (meta.icons.isEmpty()) {
            confirmationView.setDefaultIcon()
            R.drawable.ic_services
        } else {
            confirmationView.setIcon(meta.icons[FIRST_ICON])
            meta.icons[FIRST_ICON]
        }

}