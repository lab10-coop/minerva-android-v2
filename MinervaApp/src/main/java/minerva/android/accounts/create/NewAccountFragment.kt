package minerva.android.accounts.create

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.accounts.adapter.AddressesAdapter
import minerva.android.accounts.adapter.NetworkSpinnerAdapter
import minerva.android.databinding.FragmentNewAccountBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.manager.networks.NetworkManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewAccountFragment : BaseFragment(R.layout.fragment_new_account) {

    private lateinit var binding: FragmentNewAccountBinding
    private val viewModel: NewAccountViewModel by viewModel()
    private val addressAdapter = AddressesAdapter()

    private val networkSpinnerAdapter by lazy {
        NetworkSpinnerAdapter(
            requireContext(),
            R.layout.spinner_network,
            NetworkManager.networks.filter { it.testNet == !viewModel.areMainNetsEnabled }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewAccountBinding.bind(view)
        setupRecycleView()
        initializeFragment()
        setupCreateButton()
    }

    private fun initializeFragment() {
        binding.networksHeader.text = getHeader(viewModel.areMainNetsEnabled)
        viewModel.apply {
            createAccountLiveData.observe(viewLifecycleOwner, EventObserver { activity?.finish() })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { handleAutomaticBackupError(it, noAutomaticBackupErrorAction = { activity?.finish() }) })
            refreshAddressesLiveData.observe(viewLifecycleOwner, EventObserver { refreshAddressAdapterList() })
        }
    }

    private fun handleLoader(isShowing: Boolean) {
        binding.apply {
            addAccountProgressBar.visibleOrGone(isShowing)
            createButton.visibleOrGone(!isShowing)
        }
    }

    private fun setupRecycleView() = with(binding) {
        networkSpinner.apply {
            setBackgroundResource(R.drawable.rounded_spinner_background)
            adapter = networkSpinnerAdapter.apply { setDropDownViewResource(R.layout.spinner_token) }
            setPopupBackgroundResource(R.drawable.rounded_white_background)
            setSelection(viewModel.selectedNetworkPosition, false).also {
                viewModel.selectedNetworkChainId = networkSpinnerAdapter.getItem(viewModel.selectedNetworkPosition).chainId
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.selectedNetworkPosition = position
                    viewModel.selectedNetworkChainId = networkSpinnerAdapter.getItem(position).chainId
                    refreshAddressAdapterList()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    setSelection(viewModel.selectedNetworkPosition, true)
                    viewModel.selectedNetworkChainId = networkSpinnerAdapter.getItem(viewModel.selectedNetworkPosition).chainId
                    refreshAddressAdapterList()
                }
            }
        }
        viewModel.unusedAddresses.let { newAddresses ->
            addressRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = addressAdapter.apply { updateList(newAddresses) }
                visibleOrGone(newAddresses.isNotEmpty())
            }
            noAddressesInfo.root.visibleOrGone(newAddresses.isEmpty())
        }
    }

    private fun refreshAddressAdapterList() = with(binding) {
        viewModel.unusedAddresses.let { newAddresses ->
            addressAdapter.updateList(newAddresses)
            createButton.isEnabled = !addressAdapter.isEmpty()
            addressRecyclerView.visibleOrGone(newAddresses.isNotEmpty())
            noAddressesInfo.root.visibleOrGone(newAddresses.isEmpty())
        }

    }

    private fun setupCreateButton() = with(binding.createButton) {
        isEnabled = !addressAdapter.isEmpty()
        setOnClickListener {
            val index = addressAdapter.getSelectedIndex()
            val network = networkSpinnerAdapter.getItem(viewModel.selectedNetworkPosition)
            viewModel.connectAccountToNetwork(index, network)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewAccountFragment()
    }
}
