package minerva.android.onboarding.create

import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentCreateWalletBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateWalletFragment : BaseOnBoardingFragment(R.layout.fragment_create_wallet) {

    private val viewModel: CreateWalletViewModel by viewModel()
    lateinit var binding: FragmentCreateWalletBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateWalletBinding.bind(view)
        setupIdentityItem()
        setupValuesItem()
        setupServicesItem()
        handleCreateWalletButton()
    }

    override fun onResume() {
        super.onResume()
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
    }

    private fun showLoader() = with(binding) {
        createWalletProgressBar.visible()
        createWalletButton.invisible()
    }

    private fun handleCreateWalletButton() {
        binding.createWalletButton.setOnClickListener {
            viewModel.createWalletConfig()
        }
    }

    private fun setupServicesItem() {
        binding.servicesItem.apply {
            setIcon(R.drawable.ic_services)
            setTitle(getString(R.string.services))
            setContent(getString(R.string.services_instruction))
        }
    }

    private fun setupValuesItem() {
        binding.valuesItem.apply {
            setIcon(R.drawable.ic_values)
            setTitle(getString(R.string.values))
            setContent(getString(R.string.accounts_instruction))
        }
    }

    private fun setupIdentityItem() {
        binding.identityItem.apply {
            setIcon(R.drawable.ic_identities)
            setTitle(getString(R.string.identities))
            setContent(getString(R.string.identity_instruction))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CreateWalletFragment()
    }
}
