package minerva.android.accounts.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_new_account.*
import kotlinx.android.synthetic.main.fragment_new_account.networksHeader
import minerva.android.R
import minerva.android.accounts.adapter.NetworkAdapter
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.wrapped.WrappedActivity.Companion.POSITION
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewAccountFragment : BaseFragment() {

    private val viewModel: NewAccountViewModel by viewModel()
    private val networkAdapter by lazy {
        NetworkAdapter(NetworkManager.networks.filter { it.testNet == !viewModel.areMainNetsEnabled })
    }

    private var position: Int = Int.InvalidIndex

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_new_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFragment()
        setupRecycleView()
        setupCreateButton()
    }

    private fun initializeFragment() {
        networksHeader.text = getHeader(viewModel.areMainNetsEnabled)
        viewModel.apply {
            createAccountLiveData.observe(viewLifecycleOwner, EventObserver { activity?.finish() })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { handleAutomaticBackupError(it, noAutomaticBackupErrorAction = { activity?.finish() }) })
        }
        arguments?.let {
            position = it.getInt(POSITION)
        }
    }

    private fun handleLoader(isShowing: Boolean) {
        if (isShowing) {
            addAccountProgressBar.visible()
            createButton.gone()
        } else {
            addAccountProgressBar.gone()
            createButton.visible()
        }
    }

    private fun setupRecycleView() {
        networks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = networkAdapter
        }
    }

    private fun setupCreateButton() {
        createButton.setOnClickListener {
            viewModel.createNewAccount(networkAdapter.getSelectedNetwork())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(index: Int) =
            NewAccountFragment().apply {
                arguments = Bundle().apply {
                    putInt(POSITION, index)
                }
            }
    }
}
