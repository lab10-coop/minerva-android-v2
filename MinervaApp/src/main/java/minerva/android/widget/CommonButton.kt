package minerva.android.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R

private const val INTENT_TYPE = "text/plain"
private const val FORMAT = "text label"

fun setupShareButton(shareButton: ActionButton, stringToShare: String) {
    shareButton.apply {
        setIcon(R.drawable.ic_share)
        setLabel(context.getString(R.string.share))
        setOnClickListener {
            Intent(Intent.ACTION_SEND).run {
                type = INTENT_TYPE
                putExtra(Intent.EXTRA_TEXT, stringToShare)
                context.startActivity(Intent.createChooser(this, context.getString(R.string.share_via)))
            }
        }
    }
}

fun setupCopyButton(copyButton: ActionButton, stringToCopy: String, message: String) {
    copyButton.apply {
        setIcon(R.drawable.ic_copy)
        setLabel(context.getString(R.string.copy))
        setOnClickListener {
            copyStringToClipBoard(context, stringToCopy)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Get String From Clipboard - method which get and return data from clipboard (buffered)
 * @return String - data from clipboard or empty string
 */
fun getStringFromClipboard(context: Context): String {
    val clipboard: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    return clipboard?.primaryClip?.getItemAt(0)?.text.toString()
}

private fun copyStringToClipBoard(context: Context, stringToCopy: String) {
    (context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText(FORMAT, stringToCopy)
    )
}