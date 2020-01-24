package minerva.android.values.transaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.fragment_transactions.*
import minerva.android.R
import minerva.android.extension.getCurrentFragment
import minerva.android.values.listener.TransactionFragmentsListener
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.widget.repository.getNetworkIcon

class TransactionActivity : AppCompatActivity(), TransactionFragmentsListener {

    private lateinit var value: Value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        getValueFromIntent()
        prepareActionBar()
        showTransactionFragment()
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    private fun showTransactionFragment() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.transactionFragmentsContainer, TransactionsFragment.newInstance(value.network))
            commit()
        }
    }

    private fun getValueFromIntent() {
        intent.getSerializableExtra(VALUE)?.apply { value = this as Value }
    }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            show()
            title = " ${getString(R.string.send)} ${value.network}"
            subtitle = " ${value.name}"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(true)
            setLogo(getDrawable(getNetworkIcon(Network.fromString(value.network))))
        }
    }

    override fun onResult(isResultSucceed: Boolean) {
        setResult(Activity.RESULT_OK, Intent().putExtra(IS_TRANSACTION_SUCCESS, true))
        finish()
    }

    override fun showScanner() {
        supportActionBar?.hide()
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.animator.slide_in_left, 0, 0, R.animator.slide_out_right)
            replace(R.id.transactionFragmentsContainer, TransactionScannerFragment.newInstance())
            addToBackStack(null)
            commit()
        }
    }

    override fun setScanResult(text: String?) {
        onBackPressed()
        (getCurrentFragment() as TransactionsFragment).receiver.setText(text)
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
        const val VALUE = "value"
    }
}
