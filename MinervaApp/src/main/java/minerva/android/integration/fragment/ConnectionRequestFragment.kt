package minerva.android.integration.fragment


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_connection_request.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.integration.ThirdPartyRequestViewModel
import minerva.android.integration.listener.PaymentCommunicationListener
import minerva.android.kotlinUtils.event.EventObserver
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConnectionRequestFragment : Fragment() {

    private val viewModel: ThirdPartyRequestViewModel by sharedViewModel()
    private lateinit var listener: PaymentCommunicationListener


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_connection_request, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manualLabel.text = viewModel.connectionReason
        connectionRequestView.setRequestedData(viewModel.requestedData)
        setOnAllowButtonOnClickListener()
        setOnDenyButtonOnClickListener()
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.apply {
            addedNewServiceLiveData.observe(viewLifecycleOwner, EventObserver { listener.onNewServicesConnected() })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
        }
    }

    private fun handleLoader(shouldShow: Boolean) {
        requestProgressBar.visibleOrGone(shouldShow)
        progressBarGroup.visibleOrGone(!shouldShow)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PaymentCommunicationListener
    }

    private fun setOnAllowButtonOnClickListener() {
        allowButton.setOnClickListener {
            viewModel.connectToService()
        }
    }

    private fun setOnDenyButtonOnClickListener() {
        denyButton.setOnClickListener {
            listener.onDeny()
        }
    }
//todo maybe needed for m27 integration
//    private fun setConnectionLabel() {
//        val label =
//            "{`${viewModel.payment.url}` < text-style:bold /> } ${getString(R.string.connection_label)} {`${getString(R.string.minerva)}` < text-style:bold />}".spannify()
//        connectionLabel.text = label
//    }

    companion object {
        @JvmStatic
        fun newInstance() = ConnectionRequestFragment()
    }
}
