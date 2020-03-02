package minerva.android.payment.fragment


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_confirm_transaction.*
import minerva.android.R
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.payment.PaymentRequestViewModel
import minerva.android.payment.listener.FingerPrintListener
import minerva.android.payment.listener.PaymentCommunicationListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConfirmTransactionFragment : Fragment(), FingerPrintListener {

    private val viewModel: PaymentRequestViewModel by sharedViewModel()
    private lateinit var listener: PaymentCommunicationListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_confirm_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPayment()
        setFingerPrintView()
        viewModel.confirmPaymentLiveData.observe(this, EventObserver { listener.onResultOk(it) })
    }

    private fun setFingerPrintView() {
        fingerPrint.run {
            setListener(this@ConfirmTransactionFragment)
            setOnClickListeners()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PaymentCommunicationListener
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

    override fun onCancelClicked() {
        activity?.finish()
    }

    override fun onFingerPrintClicked() {
        viewModel.confirmTransaction()
    }
}
