package minerva.android.services.login.identity


import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentChooseIdentityBinding
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

class ChooseIdentityFragment : Fragment(R.layout.fragment_choose_identity) {

    private val viewModel: ChooseIdentityViewModel by viewModel()
    private val identitiesAdapter = IdentitiesAdapter()
    private lateinit var serviceQrCode: ServiceQrCode
    private lateinit var listener: LoginScannerListener
    private lateinit var binding: FragmentChooseIdentityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            serviceQrCode = it.getParcelable<QrCode>(SERVICE_QR_CODE) as ServiceQrCode
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChooseIdentityBinding.bind(view)
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

    private fun hideLoader() = with(binding) {
        loginButton.visible()
        loginProgressBar.invisible()
    }

    private fun showLoader() = with(binding) {
        loginProgressBar.visible()
        loginButton.invisible()
    }

    private fun setupServiceData() = with(binding) {
        minervaPrimitiveName.text = serviceQrCode.serviceName
        requestedData.prepareChain(
            serviceQrCode.requestedData,
            getString(R.string.requested_data),
            getString(R.string.did)
        )
    }

    private fun setupIdentitiesList() {
        viewModel.getIdentities()?.let {
            identitiesAdapter.updateList(it.toMutableList())
        }
    }

    private fun setLoginButtonOnClickListener() {
        binding.loginButton.setOnClickListener {
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

    private fun setupRecycleView() = with(binding) {
        identities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = identitiesAdapter
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onPainlessLoginResult(false, LoginPayload(getLoginStatus(it))) })
            loginLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onPainlessLoginResult(true, payload = it) })
            requestedFieldsLiveData.observe(viewLifecycleOwner, EventObserver { handleRequestedFields() })
        }
    }

    private fun handleRequestedFields() {
        identitiesAdapter.getSelectedIdentity()?.let {
            if (it.isNewIdentity) startNewIdentityOnResultWrappedActivity(activity, serviceQrCode)
            else startEditIdentityOnResultWrappedActivity(
                activity,
                viewModel.getIdentityPosition(it.index),
                it.name,
                serviceQrCode
            )
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
