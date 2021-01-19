package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import minerva.android.R
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.Empty
import minerva.android.walletConnect.model.session.WCPeerMeta

class DappConfirmationDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    private val binding: DappConfirmationDialogBinding =
        DappConfirmationDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        setCancelable(false)
        with(binding) {
            confirmationView.hideRequestedData()
            cancel.setOnClickListener {
                deny()
                dismiss()
            }
            connect.setOnClickListener {
                approve()
                dismiss()
            }
        }
        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                deny()
                dismiss()
            }
            true
        }
    }

    fun setView(meta: WCPeerMeta) = with(binding) {
        Glide.with(context)
            .load(getIcon(meta))
            .into(icon)
        name.text = meta.name
    }

    private fun DappConfirmationDialogBinding.getIcon(meta: WCPeerMeta): Any =
        if (meta.icons.isEmpty()) {
            confirmationView.setDefaultIcon()
            R.drawable.ic_services
        } else {
            confirmationView.setIcon(meta.icons[FIRST_ICON])
            meta.icons[FIRST_ICON]
        }

    fun setNetworkName(name: String) {
        binding.network.text = name
    }

    fun setNotDefinedNetwork() {
        with(binding.network) {
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