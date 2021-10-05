package minerva.android.onboarding.welcome

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentWelcomeBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import minerva.android.onboarding.create.CreateWalletViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class WelcomeFragment : BaseOnBoardingFragment(R.layout.fragment_welcome) {

    private lateinit var binding: FragmentWelcomeBinding
    private val viewModel: CreateWalletViewModel by viewModel()

    override fun onResume() {
        super.onResume()
        listener.updateActionBar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWelcomeBinding.bind(view)
        handleCreateWalletButton()
        handleRestoreWalletButton()
        setupTermsLink()
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.apply {
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            createWalletLiveData.observe(viewLifecycleOwner, EventObserver { listener.showMainActivity() })
        }
    }

    private fun hideLoader() = with(binding) {
        createWalletButton.visible()
        createWalletProgressBar.invisible()
        restoreWalletButton.isEnabled = true
    }

    private fun showLoader() = with(binding) {
        createWalletProgressBar.visible()
        createWalletButton.invisible()
        restoreWalletButton.isEnabled = false
    }

    private fun handleCreateWalletButton() {
        binding.createWalletButton.setOnClickListener {
            viewModel.createWalletConfig()
        }
    }

    private fun handleRestoreWalletButton() {
        binding.restoreWalletButton.setOnClickListener {
            listener.showRestoreWalletFragment()
        }
    }

    private fun setupTermsLink() {
        binding.termsOfService.movementMethod = LinkMovementMethod.getInstance()
    }

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeFragment()
    }
}
