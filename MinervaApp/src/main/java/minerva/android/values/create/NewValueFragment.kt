package minerva.android.values.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_new_value.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.values.adapter.NetworkAdapter
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity.Companion.POSITION
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class NewValueFragment : Fragment() {

    private val viewModel: NewValueViewModel by viewModel()
    private val networkAdapter = NetworkAdapter()

    private var position: Int = Int.InvalidIndex

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_new_value, container, false)

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

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun initializeFragment() {
        viewModel.apply {
            createValueLiveData.observe(viewLifecycleOwner, EventObserver { activity?.finish() })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoader(it) })
            saveErrorLiveData.observe(viewLifecycleOwner, EventObserver { showError(it) })
        }
        arguments?.let {
            position = it.getInt(POSITION)
        }
    }

    private fun handleLoader(isShowing: Boolean) {
        if (isShowing) {
            addValueProgressBar.visible()
            createButton.gone()
        } else {
            addValueProgressBar.gone()
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
            viewModel.createNewValue(networkAdapter.getSelectedNetwork(), position)
        }
    }

    private fun showError(throwable: Throwable) {
        Timber.e(throwable)
        MinervaFlashbar.show(requireActivity(), getString(R.string.creating_value_error), getString(R.string.unexpected_error))
    }

    companion object {
        @JvmStatic
        fun newInstance(index: Int) =
            NewValueFragment().apply {
                arguments = Bundle().apply {
                    putInt(POSITION, index)
                }
            }
    }
}
