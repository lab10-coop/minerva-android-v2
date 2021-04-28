package minerva.android.settings.authentication

import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentAuthenticationBinding
import minerva.android.extensions.showBiometricPrompt
import minerva.android.main.base.BaseFragment
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationFragment : BaseFragment(R.layout.fragment_authentication) {

    private lateinit var binding: FragmentAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAuthenticationBinding.bind(view)
        initializeFragment()
    }

    override fun onPause() {
        super.onPause()
        viewModel.wasCredentialsChecked = false
    }

    private fun initializeFragment() {
        binding.apply {
            protectKeysSwitch.isChecked = viewModel.isProtectKeysEnabled
            protectTransactionsSwitch.isChecked = viewModel.isProtectTransactionsEnabled
            activateProtectTransactions(viewModel.isProtectKeysEnabled)
            protectKeysContainer.setOnClickListener { showBiometric { toggleProtectKeys() } }
            protectTransactionsContainer.setOnClickListener {
                if (protectKeysSwitch.isChecked) showBiometric { toggleProtectTransaction() }
                else showWarningFlashbar()
            }
        }
    }

    private fun showBiometric(onSuccessAction: () -> Unit) {
        if (viewModel.wasCredentialsChecked) onSuccessAction()
        else activity?.let {
            (it.getSystemService(KEYGUARD_SERVICE) as KeyguardManager).let { keyguard ->
                if (keyguard.isDeviceSecure) showBiometricPrompt { onSuccessAction() }
                else MinervaFlashbar.show(it, getString(R.string.device_not_secured), getString(R.string.device_not_secured_message))
            }
        }
    }

    private fun activateProtectTransactions(isActivated: Boolean) {
        binding.protectTransactionsContainer.alpha = if (isActivated) FULL_VISIBLE else HALF_TRANSPARENT
    }

    private fun toggleProtectKeys() {
        with(viewModel) {
            toggleProtectKeys()
            wasCredentialsChecked = true
            binding.apply {
                protectKeysSwitch.toggle()
                protectKeysSwitch.isChecked.let { isProtectKeysActive ->
                    activateProtectTransactions(isProtectKeysActive)
                    if (isProtectKeysActive) protectTransactionsContainer.isEnabled = true
                }
                protectTransactionsSwitch.isChecked = isProtectTransactionsEnabled
            }
        }
    }

    private fun toggleProtectTransaction() {
        binding.protectTransactionsSwitch.toggle()
        viewModel.apply {
            toggleProtectTransactions()
            wasCredentialsChecked = true
        }
    }

    private fun showWarningFlashbar() {
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