package minerva.android.settings.authentication

import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentAuthenticationBinding
import minerva.android.main.base.BaseFragment
import minerva.android.widget.MinervaFlashbar
import minerva.android.extensions.showBiometricPrompt
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationFragment : BaseFragment(R.layout.fragment_authentication) {

    private lateinit var binding: FragmentAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAuthenticationBinding.bind(view)
        initializeFragment()
    }

    private fun initializeFragment() {
        binding.apply {
            authenticationSwitch.isChecked = viewModel.isAuthenticationEnabled()
            tapContainer.setOnClickListener { toggleAuthentication() }
        }
    }

    private fun toggleAuthentication() {
        activity?.let {
            (it.getSystemService(KEYGUARD_SERVICE) as KeyguardManager).let { keyguard ->
                if (keyguard.isDeviceSecure) showBiometricPrompt { toggleAuthenticationSwitch() }
                else MinervaFlashbar.show(it, getString(R.string.device_not_secured), getString(R.string.device_not_secured_message))
            }
        }
    }

    private fun toggleAuthenticationSwitch() {
        binding.authenticationSwitch.toggle()
        viewModel.toggleAuthentication()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AuthenticationFragment()
    }
}