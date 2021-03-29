package minerva.android.settings.authentication

import android.os.Bundle
import android.util.Log
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentAuthenticationBinding
import minerva.android.main.base.BaseFragment
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
            tapContainer.setOnClickListener {
                authenticationSwitch.toggle()
                viewModel.toggleAuthentication()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AuthenticationFragment()
    }
}