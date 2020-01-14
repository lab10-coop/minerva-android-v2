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
private const val TITLE = "Share via"

fun setupShareButton(shareButton: ActionButton, stringToShare: String) {
    shareButton.apply {
        setIcon(R.drawable.ic_share)
        setLabel(context.getString(R.string.share))
        setOnClickListener {
            Intent(Intent.ACTION_SEND).run {
                type = INTENT_TYPE
                putExtra(Intent.EXTRA_TEXT, stringToShare)
                shareButton.context.startActivity(Intent.createChooser(this, TITLE))
            }
        }
    }
}

fun setupCopyButton(copyButton: ActionButton, stringToCopy: String) {
    copyButton.apply {
        setIcon(R.drawable.ic_copy)
        setLabel(context.getString(R.string.copy))
        setOnClickListener {
            copyMnemonicToClipBoard(context, stringToCopy)
            Toast.makeText(this.context, context.getString(R.string.mnemonic_saved_to_clip_board), Toast.LENGTH_LONG).show()
        }
    }
}

private fun copyMnemonicToClipBoard(context: Context, publicKey: String) {
    (context?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
        ClipData.newPlainText(FORMAT, publicKey)
    )
}