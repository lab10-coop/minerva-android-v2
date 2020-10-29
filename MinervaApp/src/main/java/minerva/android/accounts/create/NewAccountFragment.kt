package minerva.android.accounts.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_new_account.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.accounts.adapter.NetworkAdapter
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity.Companion.POSITION
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class NewAccountFragment : Fragment() {

    private val viewModel: NewAccountViewModel by viewModel()
    private val networkAdapter = NetworkAdapter()

    private var position: Int = Int.InvalidIndex

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_new_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFragment()
        setupRecycleView()
        setupCreateButton()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun initializeFragment() {
        viewModel.apply {
            createAccountLiveData.observe(viewLifecycleOwner, EventObserver { activity?.finish() })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
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
            viewModel.createNewAccount(networkAdapter.getSelectedNetwork(), position)
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
