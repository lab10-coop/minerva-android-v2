package minerva.android.settings.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_backup.*
import minerva.android.R
import minerva.android.kotlinUtils.event.EventObserver
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupActivity : AppCompatActivity() {

    private val viewModel: BackupViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_backup)
        setupActionBar()
        setupCopyButton()
        setupShareButton()
        setupRememberButton()
    }

    override fun onResume() {
        super.onResume()
        viewModel.apply {
            showMnemonic()
            showMnemonicLiveData.observe(this@BackupActivity, EventObserver { mnemonicTextView.text = it })
            showMnemonicErrorLiveData.observe(this@BackupActivity, EventObserver {
                Toast.makeText(this@BackupActivity, getString(R.string.retrieving_mnemonic_error), Toast.LENGTH_LONG).show()
            })
        }
    }

    private fun setupShareButton() {
        shareButton.apply {
            setIcon(R.drawable.ic_share)
            setLabel(getString(R.string.share))
            setOnClickListener {
                Intent(Intent.ACTION_SEND).run {
                    type = INTENT_TYPE
                    putExtra(Intent.EXTRA_TEXT, viewModel.mnemonic)
                    startActivity(Intent.createChooser(this, TITLE))
                }
            }
        }
    }

    private fun setupCopyButton() {
        copyButton.apply {
            setIcon(R.drawable.ic_copy)
            setLabel(getString(R.string.copy))
            setOnClickListener {
                copyMnemonicToClipBoard()
                Toast.makeText(this@BackupActivity, getString(R.string.mnemonic_saved_to_clip_board), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRememberButton() {
        rememberButton.setOnClickListener {
            viewModel.saveIsMnemonicRemembered()
            onBackPressed()
        }
    }

    private fun copyMnemonicToClipBoard() {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            ClipData.newPlainText(
                FORMAT,
                viewModel.mnemonic
            )
        )
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = getString(R.string.account_backup)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    companion object {
        private const val FORMAT = "text label"
        private const val INTENT_TYPE = "text/plain"
        private const val TITLE = "Share via"
    }
}
