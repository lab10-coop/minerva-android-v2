package minerva.android.values.transaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_transaction.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.widget.repository.getNetworkIcon

class TransactionActivity : AppCompatActivity() {

    private lateinit var value: Value
    private lateinit var networkName: String
    private var areTransactionCostsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        getValueFromIntent()
        prepareActionBar()
        setupTexts()
        setupListeners()
    }

    private fun setupListeners() {
        setSendButtonOnClickListener()
        setOnTransactionCostOnClickListener()
        setGetAllBalanceListener()
        setAddressScannerListener()
    }

    private fun setAddressScannerListener() {
//        todo add getting all balance
        amount.onRightDrawableClicked {
            Toast.makeText(this, "ALL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setGetAllBalanceListener() {
//        todo add showing scanner
        receiver.onRightDrawableClicked {
            Toast.makeText(this, "Scanner", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setOnTransactionCostOnClickListener() {
        transactionCostLayout.setOnClickListener {
            TransitionManager.beginDelayedTransition(transactionView)
            if (areTransactionCostsOpen) closeTransactionCost() else openTransactionCost()
        }
    }

    private fun openTransactionCost() {
        areTransactionCostsOpen = true
        transactionCostLayout.apply {
            arrow.rotate180()
            gasPriceInputLayout.visible()
            gasLimitInputLayout.visible()
        }
    }

    private fun closeTransactionCost() {
        areTransactionCostsOpen = false
        transactionCostLayout.apply {
            arrow.rotate180back()
            gasPriceInputLayout.gone()
            gasLimitInputLayout.gone()
        }
    }

    private fun getValueFromIntent() {
        intent.getSerializableExtra(VALUE)?.apply {
            value = this as Value
            networkName = this.network
        }
    }

    private fun setSendButtonOnClickListener() {
//        todo handle transaction result
        sendButton.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().putExtra(IS_TRANSACTION_SUCCESS, true))
            finish()
        }
    }

    private fun setupTexts() {
        amountInputLayout.hint = "${getString(R.string.amount)}($networkName)"
        sendButton.text = "${getString(R.string.send)} $networkName"
//        todo add transaction cost
        transactionCost.text = "${transactionCost.text} ~0.01 $networkName"
    }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            title = " ${getString(R.string.send)} $networkName"
            subtitle = " ${value.name}"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(true)
            setLogo(getDrawable(getNetworkIcon(Network.fromString(networkName))))
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
        const val VALUE = "value"
    }
}
