package minerva.android.onboarding.welcome

import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentWelcomeBinding
import minerva.android.onboarding.base.BaseOnBoardingFragment


class WelcomeFragment : BaseOnBoardingFragment(R.layout.fragment_welcome) {

    private lateinit var binding: FragmentWelcomeBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWelcomeBinding.bind(view)
        handleCreateWalletButton()
        handleRestoreWalletButton()
    }

    private fun handleCreateWalletButton() {
        binding.createWalletButton.setOnClickListener {
            listener.showCreateWalletFragment()
        }
    }

    private fun handleRestoreWalletButton() {
        binding.restoreWalletButton.setOnClickListener {
            listener.showRestoreWalletFragment()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeFragment()
    }
}
