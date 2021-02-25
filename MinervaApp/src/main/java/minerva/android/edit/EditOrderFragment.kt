package minerva.android.edit

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.accounts.listener.OnBackListener
import minerva.android.databinding.FragmentEditOrderBinding
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditOrderFragment : BaseFragment(R.layout.fragment_edit_order) {

    private var type: Int = Int.InvalidIndex
    private lateinit var onBackListener: OnBackListener
    private val viewModel: EditOrderViewModel by viewModel()
    private val orderAdapter = OrderAdapter()

    private lateinit var binding: FragmentEditOrderBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditOrderBinding.bind(view)
        initializeFragment()
        setupRecyclerView()
        setupLiveData()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackListener = context as OnBackListener
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    fun saveNewOrder() {
        viewModel.saveChanges(type, orderAdapter.getList())
    }

    private fun initializeFragment() {
        arguments?.let {
            type = it.getInt(TYPE)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(it.context)
            it.adapter = orderAdapter
            DragManageAdapter(
                orderAdapter,
                ItemTouchHelper.UP.or(ItemTouchHelper.DOWN),
                NO_SWIPE
            ).let { drag -> ItemTouchHelper(drag).attachToRecyclerView(it) }
        }
    }

    private fun setupLiveData() {
        viewModel.apply {
            walletConfigLiveData.observe(
                viewLifecycleOwner,
                Observer { orderAdapter.updateList(prepareList(type), areMainNetsEnabled) })
            viewModel.saveNewOrderLiveData.observe(viewLifecycleOwner, EventObserver { onBackListener.onBack() })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { handleAutomaticBackupError(it, { activity?.finish() }, { activity?.finish() }) })
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val NO_SWIPE = 0

        @JvmStatic
        fun newInstance(type: Int) =
            EditOrderFragment().apply {
                arguments = Bundle().apply {
                    putInt(TYPE, type)
                }
            }
    }
}
