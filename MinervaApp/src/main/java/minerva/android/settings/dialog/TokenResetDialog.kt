package minerva.android.settings.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import minerva.android.R
import minerva.android.databinding.TokenResetDialogBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.visibleOrInvisible

class TokenResetDialog(context: Context, onClear: () -> Unit) :
    Dialog(context, R.style.MaterialDialogStyle) {

    private val binding: TokenResetDialogBinding = TokenResetDialogBinding.inflate(layoutInflater)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        with(binding) {
            setContentView(root)
            cancel.setOnClickListener {
                dismiss()
            }

            clear.setOnClickListener {
                onClear()
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
        handleProgress(false)
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
            cancel.isEnabled = areEnabled
            clear.isEnabled = areEnabled
        }
    }


}