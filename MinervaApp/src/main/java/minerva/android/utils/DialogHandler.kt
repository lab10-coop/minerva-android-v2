package minerva.android.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import minerva.android.R

object DialogHandler {
    fun showDialog(context: Context, title: String, message: String, positiveAction: () -> Unit = {}) {
        defaultDialog(context, title, message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                positiveAction()
                dialog.dismiss()
            }
            .show()
    }

    fun showRemoveDialog(context: Context, title: String, message: String, positiveAction: () -> Unit) {
        defaultDialog(context, title, message)
            .setPositiveButton(R.string.remove) { dialog, _ ->
                positiveAction()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun defaultDialog(context: Context, title: String, message: String): MaterialAlertDialogBuilder =
        MaterialAlertDialogBuilder(context, R.style.AlertDialogMaterialTheme)
            .setBackground(context.getDrawable(R.drawable.rounded_white_background))
            .setTitle(title)
            .setMessage(message)
}