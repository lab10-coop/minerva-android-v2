package minerva.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import minerva.android.walletmanager.WalletManager
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext.get

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val walletManager: WalletManager by inject()
        val key = walletManager.masterKey()

        Log.e("klop", "key: $key")
    }
}
