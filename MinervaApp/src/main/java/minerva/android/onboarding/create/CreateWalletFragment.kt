package minerva.android.onboarding.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_create_wallet.*
import minerva.android.R
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateWalletFragment : BaseOnBoardingFragment() {

    private val viewModel: CreateWalletViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_create_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIdentityItem()
        setupValuesItem()
        setupServicesItem()
        handleCreateWalletButton()
    }

    override fun onResume() {
        super.onResume()
        prepareObservers()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun prepareObservers() {
        viewModel.apply {
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                Toast.makeText(context, getString(R.string.creating_wallet_error_message), Toast.LENGTH_LONG).show()
            })
            createWalletLiveData.observe(viewLifecycleOwner, EventObserver { listener.showMainActivity() })
        }
    }

    private fun hideLoader() {
        createWalletButton.visible()
        createWalletProgressBar.invisible()
    }

    private fun showLoader() {
        createWalletProgressBar.visible()
        createWalletButton.invisible()
    }

    private fun handleCreateWalletButton() {
        createWalletButton.setOnClickListener {
            viewModel.createMasterSeed()
        }
    }

    private fun setupServicesItem() {
        servicesItem.apply {
            setIcon(R.drawable.ic_services)
            setTitle(getString(R.string.services))
            setContent(getString(R.string.services_instruction))
        }
    }

    private fun setupValuesItem() {
        valuesItem.apply {
            setIcon(R.drawable.ic_values)
            setTitle(getString(R.string.accounts))
            setContent(getString(R.string.accounts_instruction))
        }
    }

    private fun setupIdentityItem() {
        identityItem.apply {
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
