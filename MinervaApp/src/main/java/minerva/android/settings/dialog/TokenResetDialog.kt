package minerva.android.settings.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import minerva.android.R
import minerva.android.databinding.TokenResetDialogBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.extension.visibleOrInvisible

class TokenResetDialog(context: Context, onConfirm: () -> Unit) :
    Dialog(context, R.style.MaterialDialogStyle) {

    private val binding: TokenResetDialogBinding = TokenResetDialogBinding.inflate(layoutInflater)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        with(binding) {
            setContentView(root)
            buttons.cancel.setOnClickListener {
                dismiss()
            }

            buttons.confirm.setOnClickListener {
                onConfirm()
                handleProgress(true)
            }
        }
    }

    private fun handleProgress(isShown: Boolean) {
        with(binding) {
            progress.visibleOrInvisible(isShown)
            handleButtons(!isShown)
        }
    }

    override fun show() {
        binding.error.invisible()
        super.show()
    }

    fun showError(throwable: Throwable?) {
        with(binding) {
            error.visible()
            throwable?.message?.let { error.text = it }
            handleProgress(false)
        }
    }

    private fun handleButtons(areEnabled: Boolean) {
        with(binding) {
            buttons.confirm.isEnabled = areEnabled
            buttons.cancel.isEnabled = areEnabled
        }
    }


}