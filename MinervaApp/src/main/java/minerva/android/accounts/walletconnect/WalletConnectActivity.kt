package minerva.android.accounts.walletconnect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.databinding.ActivityWalletConnectBinding
import minerva.android.extension.addFragment
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX
import minerva.android.utils.exhaustive
import org.koin.androidx.viewmodel.ext.android.viewModel

class WalletConnectActivity : AppCompatActivity() {

    private val binding: ActivityWalletConnectBinding by lazy {
        ActivityWalletConnectBinding.inflate(
            layoutInflater
        )
    }
    val viewModel: WalletConnectViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.getAccount(intent.getIntExtra(ACCOUNT_INDEX, Int.InvalidValue))
        supportActionBar?.hide()
        addFragment(R.id.wcContainer, WalletConnectScannerFragment())
        observeViewState()
    }

    private fun observeViewState() {
        viewModel.viewStateLiveData.observe(this, Observer {
            if (it == CloseScannerState) {
                onBackPressed()
            }
        })
    }
}