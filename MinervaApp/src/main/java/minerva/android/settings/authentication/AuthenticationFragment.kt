package minerva.android.settings.authentication

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentAuthenticationBinding
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationFragment : BaseFragment(R.layout.fragment_authentication) {

    private lateinit var binding: FragmentAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAuthenticationBinding.bind(view)
        viewModel.init()
        initializeFragment()
    }

    override fun onResume() {
        super.onResume()
        checkSystemAuthentication()
    }

    override fun onPause() {
        super.onPause()
        viewModel.wasCredentialsChecked = false
    }

    private fun initializeFragment() {
        binding.apply {
            protectKeysContainer.setOnClickListener { showBiometric { toggleProtectKeys() } }
            protectTransactionsContainer.setOnClickListener {
                if (protectKeysSwitch.isChecked) showBiometric { toggleProtectTransaction() }
                else showProtectTransactionsWarning()
            }
            with(viewModel) {
                protectKeysLiveData.observe(viewLifecycleOwner, EventObserver {
                    protectKeysSwitch.isChecked = it
                    activateProtectTransactions(it)
                    if (it) protectTransactionsContainer.isEnabled = true
                })
                protectTransactionsLiveData.observe(viewLifecycleOwner, EventObserver {
                    protectTransactionsSwitch.isChecked = it
                })
            }
        }
    }

    private fun checkSystemAuthentication() {
        if (viewModel.isProtectKeysEnabled)
            (requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).let { keyguard ->
                if (!keyguard.isDeviceSecure) viewModel.toggleProtectKeys()
            }
    }

    private fun showBiometric(onSuccessAction: () -> Unit) {
        activity?.let {
            if (viewModel.wasCredentialsChecked) onSuccessAction()
            else showBiometricPrompt(
                { onSuccessAction() },
                { MinervaFlashbar.show(it, getString(R.string.device_not_secured), getString(R.string.device_not_secured_message)) })
        }
    }

    private fun activateProtectTransactions(isActivated: Boolean) {
        binding.protectTransactionsContainer.alpha = if (isActivated) FULL_VISIBLE else HALF_TRANSPARENT
    }

    private fun toggleProtectKeys() = viewModel.run {
        wasCredentialsChecked = true
        toggleProtectKeys()
    }

    private fun toggleProtectTransaction() {
        viewModel.apply {
            toggleProtectTransactions()
            wasCredentialsChecked = true
        }
    }

    private fun showProtectTransactionsWarning() {
        MinervaFlashbar.show(
            requireActivity(),
            getString(R.string.protect_transaction_error_title),
            getString(R.string.protect_transaction_error_message)
        )
        binding.protectTransactionsContainer.isEnabled = false
    }

    companion object {
        @JvmStatic
        fun newInstance() = AuthenticationFragment()
        private const val HALF_TRANSPARENT = 0.5f
        private const val FULL_VISIBLE = 1.0f
    }
}