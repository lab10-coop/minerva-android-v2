package minerva.android.integration

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_third_party_integration.*
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.gone
import minerva.android.extension.replaceFragment
import minerva.android.extension.visible
import minerva.android.integration.fragment.ConfirmTransactionFragment
import minerva.android.integration.fragment.ConnectionRequestFragment
import minerva.android.integration.listener.PaymentCommunicationListener
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.CredentialRequest
import minerva.android.walletmanager.model.defs.PaymentRequest.Companion.SIGNED_PAYLOAD
import minerva.android.walletmanager.model.state.VCRequestState
import org.koin.androidx.viewmodel.ext.android.viewModel

class ThirdPartyIntegrationActivity : AppCompatActivity(), PaymentCommunicationListener {

    private val viewModel: ThirdPartyRequestViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_party_integration)
        checkIfWalletExists(intent.data?.encodedPath?.removePrefix(SLASH))
    }

    private fun checkIfWalletExists(jwt: String?) {
        with(viewModel) {
            if (isMasterSeedAvailable()) {
                initWalletConfig()
                prepareObservers()
                setupActionBar()
                decodeJwtToken(jwt)
            } else {
                sendResult(ON_WALLET_NOT_INITIALISED)
            }
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            showServiceConnectionRequestLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver {
                when (it) {
                    is VCRequestState.Found -> handleVCFound(it)
                    is VCRequestState.NotFound -> sendResult(ON_CREDENTIAL_NOT_FOUND)
                }
            })
            showPaymentConfirmationLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver { showConfirmTransactionScreen() })
            errorLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver { finish() }) //todo send proper error message to demo app
        }
    }

    private fun handleVCFound(it: VCRequestState.Found<Pair<Credential, CredentialRequest>>) {
        container.visible()
        loader.gone()
        minervaPrimitiveName.text = it.data.second.service.name
        addFragment(
            R.id.container,
            ConnectionRequestFragment.newInstance(),
            R.animator.slide_in_left,
            R.animator.slide_out_right
        )
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

    override fun onDeny() {
        sendResult(ON_DENY_REQUEST)
    }

    override fun onNewServicesConnected(token: String) {
        sendResult(ON_NEW_SERVICE_CONNECTED, Intent().apply { putExtra(JWT_TOKEN, token) })
    }

    override fun onResultOk(signedData: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(SIGNED_PAYLOAD, signedData))
        finish()
    }

    private fun sendResult(resultCode: Int, intent: Intent? = null) {
        setResult(resultCode, intent)
        finish()
    }

    companion object {
        private const val SLASH = "/"

        private const val ON_WALLET_NOT_INITIALISED = 997
        private const val ON_CREDENTIAL_NOT_FOUND = 998
        private const val ON_DENY_REQUEST = 999
        private const val ON_NEW_SERVICE_CONNECTED = 1000
        private const val JWT_TOKEN = "jwtToken"
    }
}
