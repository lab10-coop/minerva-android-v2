package minerva.android.walletConnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.main.MainActivity
import minerva.android.splash.BaseLaunchAppActivity
import timber.log.Timber
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class HandlingWalletConnectDeepLinkActivity : BaseLaunchAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
        checkWalletConfig()
    }

    override fun showMainActivity() {
        val modifiedData = modifyDeepLinkToWCFormat(intent?.data)
        Timber.i("Handling deep link with scheme: ${intent?.data} -> $modifiedData")
        launchActivity<MainActivity> {
            putExtra(BaseWalletConnectInteractionsActivity.MOBILE_WALLET_CONNECT_DATA, modifiedData)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            finish()
        }
    }

    private fun modifyDeepLinkToWCFormat(data: Uri?): Uri? {
        if (data == null) return null
        val uriString = data.toString()
        return when (data.scheme) {
            "https", "minerva" -> {
                val wcUri = uriString.substringAfter("uri=")
                Uri.parse(URLDecoder.decode(wcUri, StandardCharsets.UTF_8))
            }
            else -> data
        }
    }
}
