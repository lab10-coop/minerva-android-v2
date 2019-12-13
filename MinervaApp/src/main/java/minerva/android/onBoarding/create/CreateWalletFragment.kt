package minerva.android.onBoarding.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_create_wallet.*
import minerva.android.R
import minerva.android.onBoarding.base.BaseOnBoardingFragment

class CreateWalletFragment : BaseOnBoardingFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIdentityItem()
        setupValuesItem()
        setupServicesItem()
        handleCreateWalletButton()
    }

    private fun handleCreateWalletButton() {
        createWalletButton.setOnClickListener {
            //TODO add create wallet mechanism
            listener.showMainActivity()
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
            setTitle(getString(R.string.values))
            setContent(getString(R.string.values_onstruction))
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

        val TAG: String = this::class.java.canonicalName ?: "CreateWalletFragment"
    }
}
