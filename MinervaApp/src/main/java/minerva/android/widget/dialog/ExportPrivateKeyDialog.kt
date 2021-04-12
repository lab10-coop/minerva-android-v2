package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.ExportPrivateKeyDialogBinding
import minerva.android.extension.gone
import minerva.android.extension.visible
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
        binding.apply {
            showPrivateKeyButton.setOnClickListener { showPrivateKey() }
            privateKeyLabel.apply {
                setBodyGravity(Gravity.LEFT)
                togglePasswordTransformation()
                setTitleAndBody("${account.name} ${context.getString(R.string.private_key)}", account.privateKey)
                setupCopyButton(copyButton, account.privateKey, context.getString(R.string.private_key_saved_to_clipboard))
                setupShareButton(shareButton, account.privateKey)
            }
        }
    }

    private fun showPrivateKey() {
        binding.apply {
            TransitionManager.beginDelayedTransition(binding.root)
            showPrivateKeyButton.gone()
            privateKeyLabel.togglePasswordTransformation()
            copyButton.visible()
            shareButton.visible()
        }
    }
}