package minerva.android.extensions

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import minerva.android.R

fun Fragment.showRemoveDialog(title: String, messageId: Int, removeAction: () -> Unit) {
    context?.let { context ->
        MaterialAlertDialogBuilder(context, R.style.AlertDialogMaterialTheme)
            .setBackground(context.getDrawable(R.drawable.rounded_white_background))
            .setTitle(title)
            .setMessage(getString(messageId))
            .setPositiveButton(R.string.remove) { dialog, _ ->
                removeAction()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}