package minerva.android.settings.backup

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_backup.*
import minerva.android.R
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.widget.setupCopyButton
import minerva.android.widget.setupShareButton
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupActivity : AppCompatActivity() {

    private val viewModel: BackupViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_backup)
        prepareMnemonic()
        setupActionBar()
        setupCopyButton(copyButton, viewModel.mnemonic, getString(R.string.mnemonic_saved_to_clip_board))
        setupShareButton(shareButton, viewModel.mnemonic)
        setupRememberButton()
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun setupRememberButton() {
        continue_button.setOnClickListener {
            viewModel.saveIsMnemonicRemembered()
            onBackPressed()
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = getString(R.string.account_backup)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun prepareMnemonic() =
        viewModel.apply {
            showMnemonic()
            showMnemonicLiveData.observe(this@BackupActivity, EventObserver { mnemonicTextView.text = it })
        }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home
}
