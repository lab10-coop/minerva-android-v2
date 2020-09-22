package minerva.android.accounts.transaction.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.replaceFragment
import minerva.android.extension.validator.Validator.HEX_PREFIX
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.accounts.listener.TransactionListener
import minerva.android.accounts.transaction.TransactionsViewModel
import minerva.android.accounts.transaction.TransactionsViewModel.Companion.META_ADDRESS_SEPARATOR
import minerva.android.accounts.transaction.fragment.TransactionsFragment
import minerva.android.accounts.transaction.fragment.scanner.AddressScannerFragment
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.repository.getNetworkIcon
import org.koin.androidx.viewmodel.ext.android.viewModel

class TransactionActivity : AppCompatActivity(), TransactionListener {

    private val viewModel: TransactionsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        viewModel.getAccount(intent.getIntExtra(ACCOUNT_INDEX, Int.InvalidIndex), intent.getIntExtra(ASSET_INDEX, Int.InvalidIndex))
        initView()
    }

    private fun initView() {
        prepareActionBar()
        addFragment(R.id.container, TransactionsFragment.newInstance())
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            show()
            title = " ${getString(R.string.send)} ${viewModel.prepareTitle()}"
            subtitle = " ${viewModel.account.name}"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(true)
            setLogo()
        }
    }

    private fun ActionBar.setLogo() {
        if (viewModel.account.isSafeAccount) setLogo(getDrawable(R.drawable.ic_artis_safe_account))
        else setLogo(getDrawable(getNetworkIcon(NetworkManager.getNetwork(viewModel.account.network))))
    }

    override fun onTransactionAccepted(message: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(ACCOUNT_INDEX, viewModel.account.index)
            putExtra(IS_TRANSACTION_SUCCESS, true)
            putExtra(TRANSACTION_MESSAGE, message)
        })
        finish()
    }

    override fun onError(message: String) {
        MinervaFlashbar.show(
            this,
            getString(R.string.transaction_error_title),
            getString(R.string.transaction_error_message, message)
        )
    }


    override fun showScanner() {
        supportActionBar?.hide()
        replaceFragment(R.id.container, AddressScannerFragment.newInstance(), R.animator.slide_in_left, R.animator.slide_out_right)
    }

    override fun setScanResult(text: String?) {
        onBackPressed()
        text?.let {
            (getCurrentFragment() as TransactionsFragment).let { transactionFragment ->
                text.replace(META_ADDRESS_SEPARATOR, String.Empty).substringBefore(HEX_PREFIX).apply {
                    if (viewModel.isCorrectNetwork(this)) transactionFragment.setReceiver(viewModel.preparePrefixAddress(it, this))
                    else transactionFragment.setReceiver(it)
                }
            }
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
        const val IS_TRANSACTION_SUCCESS = "is_transaction_succeed"
        const val TRANSACTION_MESSAGE = "transaction_message"
        const val ACCOUNT_INDEX = "account_index"
        const val ASSET_INDEX = "asset_index"
    }
}
