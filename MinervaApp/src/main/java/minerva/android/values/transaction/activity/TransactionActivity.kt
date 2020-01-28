package minerva.android.values.transaction.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.extension.getCurrentFragment
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.values.listener.TransactionFragmentsListener
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.values.transaction.fragment.scanner.TransactionScannerFragment
import minerva.android.values.transaction.fragment.TransactionsFragment
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.widget.repository.getNetworkIcon
import org.koin.androidx.viewmodel.ext.android.viewModel

class TransactionActivity : AppCompatActivity(), TransactionFragmentsListener {

    private val viewModel: TransactionsViewModel by viewModel()
    private lateinit var value: Value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        viewModel.getValueLiveData.observe(this, EventObserver { initView(it) })
        getValue()
    }

    private fun initView(value: Value) {
        value.apply {
            this@TransactionActivity.value = this
            prepareActionBar()
            showTransactionFragment()
        }
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    private fun showTransactionFragment() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.transactionFragmentsContainer, TransactionsFragment.newInstance())
            commit()
        }
    }

    private fun getValue() {
        viewModel.getValue(intent.getIntExtra(VALUE_INDEX, Int.InvalidIndex))
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

    override fun onResult(isResultSucceed: Boolean, message: String) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(IS_TRANSACTION_SUCCESS, isResultSucceed)
            putExtra(TRANSACTION_MESSASGE, message)
        })
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
        (getCurrentFragment() as TransactionsFragment).setReceiver(text)
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
        const val TRANSACTION_MESSASGE = "transaction_message"
        const val VALUE_INDEX = "value_index"
    }
}
