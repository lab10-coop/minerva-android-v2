package minerva.android.payment

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_payment_request.*
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.replaceFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.payment.fragment.ConfirmTransactionFragment
import minerva.android.payment.fragment.ConnectionRequestFragment
import minerva.android.payment.listener.PaymentCommunicationListener
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.CONFIRM_ACTION
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.JWT_TOKEN
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.SIGNED_PAYLOAD
import org.koin.androidx.viewmodel.ext.android.viewModel

class PaymentRequestActivity : AppCompatActivity(), PaymentCommunicationListener {

    private val viewModel: PaymentRequestViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_request)
        checkIfWalletExists()
    }

    private fun checkIfWalletExists() {
        if (viewModel.isMasterSeedAvailable()) {
            viewModel.initWalletConfig()
            setupActionBar()
            prepareObservers()
            decodeToken()
        } else {
            finish()
        }
    }

    private fun decodeToken() {
        if (intent.action == CONFIRM_ACTION) {
            viewModel.decodeJwtToken(intent.getStringExtra(JWT_TOKEN))
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            showConnectionRequestLiveData.observe(this@PaymentRequestActivity, EventObserver {
                serviceName.text = it
                addFragment(R.id.container, ConnectionRequestFragment.newInstance(), R.animator.slide_in_left, R.animator.slide_out_right)
            })
            showPaymentConfirmationLiveData.observe(this@PaymentRequestActivity, EventObserver { showConfirmTransactionScreen() })
            errorLiveData.observe(this@PaymentRequestActivity, EventObserver { finish() })
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = String.Empty
            setBackgroundDrawable(ColorDrawable(getColor(R.color.lightGray)))
        }
        window.statusBarColor = getColor(R.color.lightGray)
    }

    override fun showConfirmTransactionScreen() {
        replaceFragment(R.id.container, ConfirmTransactionFragment.newInstance(), R.animator.slide_in_left, R.animator.slide_out_right)
    }

    override fun onResultOk(signedData: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(SIGNED_PAYLOAD, signedData))
        finish()
    }
}
