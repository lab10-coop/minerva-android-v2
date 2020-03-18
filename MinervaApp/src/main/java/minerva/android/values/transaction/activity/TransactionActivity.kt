package minerva.android.values.transaction.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.validator.Validator.HEX_PREFIX
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.values.listener.AddressFragmentsListener
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.values.transaction.fragment.TransactionsFragment
import minerva.android.values.transaction.fragment.scanner.TransactionScannerFragment
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.widget.repository.getNetworkIcon
import org.koin.androidx.viewmodel.ext.android.viewModel

class TransactionActivity : AppCompatActivity(), AddressFragmentsListener {

    private val viewModel: TransactionsViewModel by viewModel()
    private lateinit var value: Value
    private var assetIndex: Int = Int.InvalidIndex

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
        assetIndex = intent.getIntExtra(ASSET_INDEX, Int.InvalidIndex)
        viewModel.getValue(intent.getIntExtra(VALUE_INDEX, Int.InvalidIndex), assetIndex)
    }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            show()
            title = " ${getString(R.string.send)} ${prepareTitle()}"
            subtitle = " ${value.name}"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(true)
            setLogo()
        }
    }

    private fun ActionBar.setLogo() {
        if (value.isSafeAccount) {
            setLogo(getDrawable(R.drawable.ic_artis_safe_account))
        } else {
            setLogo(getDrawable(getNetworkIcon(Network.fromString(value.network))))
        }
    }

    override fun onResult(isResultSucceed: Boolean, message: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(IS_TRANSACTION_SUCCESS, isResultSucceed)
            putExtra(TRANSACTION_MESSAGE, message)
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
        text?.let {
            (getCurrentFragment() as TransactionsFragment).let { transactionFragment ->
                text.replace(META_ADDRESS_SEPARATOR, String.Empty).substringBefore(HEX_PREFIX).apply {
                    if (isCorrectNetwork(this)) transactionFragment.setReceiver(preparePrefixAddress(it, this))
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

    private fun isCorrectNetwork(prefixAddress: String) = value.name.contains(prefixAddress, true)

    private fun preparePrefixAddress(prefixAddress: String, prefix: String): String =
        prefixAddress.removePrefix(prefix).replace(META_ADDRESS_SEPARATOR, String.Empty)

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    private fun prepareTitle() = if (assetIndex != Int.InvalidIndex) value.assets[assetIndex].name else value.network

    companion object {
        const val IS_TRANSACTION_SUCCESS = "is_transaction_succeed"
        const val TRANSACTION_MESSAGE = "transaction_message"
        const val VALUE_INDEX = "value_index"
        const val ASSET_INDEX = "asset_index"
        const val META_ADDRESS_SEPARATOR = ":"
    }
}
