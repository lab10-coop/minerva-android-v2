package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.ExportPrivateKeyDialogBinding
import minerva.android.extension.toggleVisibleOrGone
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.setupCopyButton
import minerva.android.widget.setupShareButton

class ExportPrivateKeyDialog(context: Context, private val account: Account) : Dialog(context, R.style.DialogStyle) {

    private val binding = ExportPrivateKeyDialogBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(true)
        initView()
    }

    private fun initView() {
        with(binding) {
            privateKeyLabel.apply {
                //TODO add nice animation
                showPrivateKeyButton.setOnClickListener {
                    showPrivateKeyButton.text = toggleButtonText(showPrivateKeyButton.text)
                    togglePasswordTransformation()
                    copyButton.toggleVisibleOrGone()
                    shareButton.toggleVisibleOrGone()
                }
                setBodyGravity(Gravity.LEFT)
                togglePasswordTransformation()
                setTitleAndBody("${account.name} ${context.getString(R.string.private_key)}", account.privateKey)
                setupCopyButton(binding.copyButton, account.privateKey, context.getString(R.string.private_key_saved_to_clipboard))
                setupShareButton(binding.shareButton, account.privateKey)
            }
        }
    }

    private fun toggleButtonText(currentText: CharSequence): String =
        with(context) {
            getString(R.string.show_private_key).let { showPrivateKeyText ->
                if (showPrivateKeyText == currentText) getString(R.string.hide_private_key)
                else getString(R.string.show_private_key)
            }
        }
}