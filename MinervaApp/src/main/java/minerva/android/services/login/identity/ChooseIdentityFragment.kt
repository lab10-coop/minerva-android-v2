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
import minerva.android.services.login.LoginScannerListener
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.BACKUP_FAILURE
import minerva.android.services.login.uitls.LoginStatus.Companion.DEFAULT_STATUS
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.model.QrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.wrapped.startEditIdentityOnResultWrappedActivity
import minerva.android.wrapped.startNewIdentityOnResultWrappedActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseIdentityFragment : Fragment() {

    private val viewModel: ChooseIdentityViewModel by viewModel()
    private val identitiesAdapter = IdentitiesAdapter()
    private lateinit var serviceQrCode: ServiceQrCode
    private lateinit var listener: LoginScannerListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_choose_identity, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            serviceQrCode = it.getParcelable<QrCode>(SERVICE_QR_CODE) as ServiceQrCode
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareObservers()
        setLoginButtonOnClickListener()
        setupServiceData()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as LoginScannerListener
    }

    override fun onResume() {
        super.onResume()
        setupRecycleView()
        setupIdentitiesList()
    }

    fun handleLogin(index: Int, serviceQrCode: ServiceQrCode) = viewModel.handleLogin(index, serviceQrCode)

    private fun hideLoader() {
        loginButton.visible()
        loginProgressBar.invisible()
    }

    private fun showLoader() {
        loginProgressBar.visible()
        loginButton.invisible()
    }

    private fun setupServiceData() {
        minervaPrimitiveName.text = serviceQrCode.serviceName
        requestedData.prepareChain(serviceQrCode.requestedData, getString(R.string.requested_data), getString(R.string.did))
    }

    private fun setupIdentitiesList() {
        viewModel.getIdentities()?.let {
            identitiesAdapter.updateList(it.toMutableList())
        }
    }

    private fun setLoginButtonOnClickListener() {
        loginButton.setOnClickListener {
            handleLoginButton()
        }
    }

    private fun handleLoginButton() {
        identitiesAdapter.getSelectedIdentity()?.let {
            viewModel.handleLogin(it, serviceQrCode)
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
        viewModel.apply {
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver { listener.onPainlessLoginResult(false, LoginPayload(getLoginStatus(it))) })
            loginLiveData.observe(viewLifecycleOwner, EventObserver { listener.onPainlessLoginResult(true, payload = it) })
            requestedFieldsLiveData.observe(viewLifecycleOwner, EventObserver { handleRequestedFields() })
        }
    }

    private fun handleRequestedFields() {
        identitiesAdapter.getSelectedIdentity()?.let {
            if (it.isNewIdentity) startNewIdentityOnResultWrappedActivity(activity, serviceQrCode)
            else startEditIdentityOnResultWrappedActivity(activity, viewModel.getIdentityPosition(it.index), it.name, serviceQrCode)
        }
    }

    private fun getLoginStatus(it: Throwable): Int =
        if (it is AutomaticBackupFailedThrowable) {
            BACKUP_FAILURE
        } else {
            DEFAULT_STATUS
        }

    companion object {
        @JvmStatic
        fun newInstance(serviceQrCode: ServiceQrCode) = ChooseIdentityFragment().apply {
            arguments = Bundle().apply { putParcelable(SERVICE_QR_CODE, serviceQrCode) }
        }

        private const val SERVICE_QR_CODE = "service_qr_code"
    }
}
