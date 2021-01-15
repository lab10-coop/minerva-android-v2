package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import minerva.android.R
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
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
        confirmationView.setIcon(meta.icons[0])
        name.text = meta.name
        Glide.with(context)
            .load(meta.icons[0])
            .into(icon)
    }

    fun setNetworkName(name: String) {
        binding.network.text = name
    }

    fun setWarning() = with(binding) {
        manual.invisible()
        warning.visible()
        warringIcon.visible()
        with(network) {
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
}