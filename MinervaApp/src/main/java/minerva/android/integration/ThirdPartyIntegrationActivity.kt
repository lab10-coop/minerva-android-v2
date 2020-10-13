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
import minerva.android.walletmanager.model.state.ConnectionRequest
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
                    is ConnectionRequest.ServiceNotConnected -> connectService(it)
                    is ConnectionRequest.VCNotFound -> sendResult(ON_CREDENTIAL_NOT_FOUND)
                    is ConnectionRequest.ServiceConnected -> onNewServicesConnected(it.data)
                }
            })
            showPaymentConfirmationLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver { showConfirmTransactionScreen() })
            errorLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver { finish() }) //todo send proper error message to demo app
            onDenyConnectionSuccessLiveData.observe(this@ThirdPartyIntegrationActivity, EventObserver { sendResult(ON_DENY_REQUEST) })
        }
    }

    private fun connectService(it: ConnectionRequest.ServiceNotConnected<Pair<Credential, CredentialRequest>>) {
        viewModel.credentialRequest = it.data
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
        viewModel.saveDenyConnectionWalletAction()
    }

    override fun onNewServicesConnected(credentialRequest: Pair<Credential, CredentialRequest>) {
        sendResult(ON_NEW_SERVICE_CONNECTED, Intent().apply { putExtra(JWT_TOKEN, credentialRequest.first.token) })
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
