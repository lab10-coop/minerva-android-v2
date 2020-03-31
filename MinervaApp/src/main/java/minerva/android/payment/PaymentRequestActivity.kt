package minerva.android.payment

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_payment_request.*
import minerva.android.R
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

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun checkIfWalletExists() {
        if (viewModel.isMasterKeyAvailable()) {
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
                showConnectionRequestFragment()
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

    private fun showConnectionRequestFragment() {
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.animator.slide_in_left, 0, 0, R.animator.slide_out_right)
            add(R.id.fragmentContainer, ConnectionRequestFragment.newInstance())
            commit()
        }
    }

    override fun showConfirmTransactionScreen() {
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.animator.slide_in_left, 0, 0, R.animator.slide_out_right)
            replace(R.id.fragmentContainer, ConfirmTransactionFragment.newInstance())
            commit()
        }
    }

    override fun onResultOk(signedData: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(SIGNED_PAYLOAD, signedData))
        finish()
    }
}
