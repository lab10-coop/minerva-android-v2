package minerva.android.services.login.identity


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_choose_identity.*
import minerva.android.R
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.services.login.PainlessLoginFragmentListener
import minerva.android.walletmanager.model.QrCodeResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseIdentityFragment : Fragment() {

    private val viewModel: ChooseIdentityViewModel by viewModel()
    private val identitiesAdapter = IdentitiesAdapter()
    private lateinit var qrCodeResponse: QrCodeResponse
    private lateinit var listener: PainlessLoginFragmentListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_identity, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            qrCodeResponse = it.getSerializable(QR_CODE_RESPONSE) as QrCodeResponse
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareObservers()
        setLoginButtonOnClickListener()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PainlessLoginFragmentListener
    }

    override fun onResume() {
        super.onResume()
        setupServiceData()
        setupRecycleView()
        setupIdentitiesList()
    }

    private fun hideLoader() {
        loginButton.visible()
        loginProgressBar.invisible()
    }

    private fun showLoader() {
        loginProgressBar.visible()
        loginButton.invisible()
    }

    private fun setupServiceData() {
        serviceName.text = qrCodeResponse.serviceName
        requestedFields.text = qrCodeResponse.identityFields
    }

    private fun setupIdentitiesList() {
        viewModel.getIdentities()?.let {
            identitiesAdapter.updateList(it)
        }
    }

    private fun setLoginButtonOnClickListener() {
        loginButton.setOnClickListener {
            handleLoginButton()
        }
    }

    private fun handleLoginButton() {
        identitiesAdapter.getSelectedIdentity()?.let {
            viewModel.handleLoginButton(it, qrCodeResponse)
        }.orElse {
            Toast.makeText(context, getString(R.string.select_identity_message), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecycleView() {
        identities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = identitiesAdapter
        }
    }

    private fun prepareObservers() {
        viewModel.loadingLiveData.observe(this, EventObserver { if (it) showLoader() else hideLoader() })
        viewModel.errorLiveData.observe(this, EventObserver {
            listener.onLoginResult(false)
        })
        viewModel.loginLiveData.observe(this, EventObserver {
            listener.onLoginResult(true)
        })
        viewModel.requestedFieldsLiveData.observe(this, EventObserver {
            Toast.makeText(context, getString(R.string.requested_data_message), Toast.LENGTH_LONG).show()
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(qrCodeResponse: QrCodeResponse) = ChooseIdentityFragment().apply {
            arguments = Bundle().apply {
                putSerializable(QR_CODE_RESPONSE, qrCodeResponse)
            }
        }

        private const val QR_CODE_RESPONSE = "scanResult"
        val TAG: String = this::class.java.canonicalName ?: "ChooseIdentityFragment"
    }
}
