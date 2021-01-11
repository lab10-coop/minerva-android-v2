package minerva.android.accounts.create

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.accounts.adapter.NetworkAdapter
import minerva.android.databinding.FragmentNewAccountBinding
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.wrapped.WrappedActivity.Companion.POSITION
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewAccountFragment : BaseFragment(R.layout.fragment_new_account) {

    private lateinit var binding: FragmentNewAccountBinding
    private val viewModel: NewAccountViewModel by viewModel()
    private val networkAdapter by lazy {
        NetworkAdapter(NetworkManager.networks.filter { it.testNet == !viewModel.areMainNetsEnabled })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewAccountBinding.bind(view)
        initializeFragment()
        setupRecycleView()
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
        }
    }

    private fun handleLoader(isShowing: Boolean) {
        binding.apply {
            if (isShowing) {
                addAccountProgressBar.visible()
                createButton.gone()
            } else {
                addAccountProgressBar.gone()
                createButton.visible()
            }
        }
    }

    private fun setupRecycleView() {
        binding.networks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = networkAdapter
        }
    }

    private fun setupCreateButton() {
        binding.createButton.setOnClickListener {
            viewModel.createNewAccount(networkAdapter.getSelectedNetwork())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewAccountFragment()
    }
}
