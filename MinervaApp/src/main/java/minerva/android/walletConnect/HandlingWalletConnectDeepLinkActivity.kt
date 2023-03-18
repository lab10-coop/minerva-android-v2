package minerva.android.walletConnect

import android.content.Intent
import android.os.Bundle
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.main.MainActivity
import minerva.android.splash.BaseLaunchAppActivity
import timber.log.Timber

class HandlingWalletConnectDeepLinkActivity : BaseLaunchAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
        checkWalletConfig()
    }

    override fun showMainActivity() {
        Timber.i("Handling deep link with scheme: ${intent?.data?.scheme}")
        launchActivity<MainActivity> {
            putExtra(BaseWalletConnectInteractionsActivity.MOBILE_WALLET_CONNECT_DATA, intent?.data)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            finish()
        }
    }
}
