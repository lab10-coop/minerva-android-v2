package minerva.android.walletConnect

import android.content.Intent
import android.os.Bundle
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.main.MainActivity
import minerva.android.splash.BaseLaunchAppActivity

class HandlingWalletConnectDeepLinkActivity : BaseLaunchAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
        checkWalletConnect()
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> {
            putExtra(BaseWalletConnectInteractionsActivity.MOBILE_WALLET_CONNECT_DATA, intent?.data)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            finish()
        }
    }
}