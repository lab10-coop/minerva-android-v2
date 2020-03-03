package minerva.android.payment.fragment


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_confirm_transaction.*
import minerva.android.R
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.payment.PaymentRequestViewModel
import minerva.android.payment.listener.PaymentCommunicationListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConfirmTransactionFragment : Fragment() {

    private val viewModel: PaymentRequestViewModel by sharedViewModel()
    private lateinit var listener: PaymentCommunicationListener
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_confirm_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPayment()
        viewModel.confirmPaymentLiveData.observe(this, EventObserver { listener.onResultOk(it) })
        initBiometricPrompt()
        biometricPrompt.authenticate(getPromptInfoDialog())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PaymentCommunicationListener
    }

    private fun initBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity?.finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.confirmTransaction()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    activity?.finish()
                }
            })
    }

    private fun getPromptInfoDialog(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.confirm_payment))
            .setSubtitle(getString(R.string.finger_print_instruction))
            .setDeviceCredentialAllowed(true)
            .setConfirmationRequired(false)
            .build()
    }

    private fun setPayment() {
        viewModel.payment.apply {
            amountTextView.text = amount
            recipientTextView.text = recipient
            ibanTextView.text = iban
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConfirmTransactionFragment()
    }
}
