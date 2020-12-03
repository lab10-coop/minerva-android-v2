package minerva.android.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

fun Context.isIntentSafe(intent: Intent) =
    this.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

fun Context.openUri(appUri: String = String.empty, webUri: String) {
    Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).run {
        if (this@openUri.isIntentSafe(this)) {
            startActivity(this)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
        }
    }
}