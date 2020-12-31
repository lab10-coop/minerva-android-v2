package minerva.android.accounts.walletconnect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.databinding.ActivityWalletConnectBinding
import minerva.android.extension.addFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class WalletConnectActivity : AppCompatActivity() {

    private val binding: ActivityWalletConnectBinding by lazy { ActivityWalletConnectBinding.inflate(layoutInflater) }
    val viewModel: WalletConnectViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()
        addFragment(R.id.wcContainer, WalletConnectScannerFragment())
        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.viewStateLiveData.observe(this, Observer {
            when (it) {
                CloseScannerState -> onBackPressed()
            }
        })
    }
}