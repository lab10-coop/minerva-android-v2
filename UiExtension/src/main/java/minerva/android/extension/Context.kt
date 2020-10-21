package minerva.android.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

fun Context.isIntentSafe(intent: Intent) =
    this.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

fun Context.openApp(appUri: String, webUri: String) {
    Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).run {
        if (this@openApp.isIntentSafe(this)) {
            startActivity(this)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
        }
    }
}