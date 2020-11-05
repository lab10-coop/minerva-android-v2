package minerva.android.edit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.accounts.listener.OnBackListener
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditOrderFragment : BaseFragment() {

    private var type: Int = Int.InvalidIndex
    private lateinit var onBackListener: OnBackListener
    private val viewModel: EditOrderViewModel by viewModel()
    private val orderAdapter = OrderAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit_order, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        recyclerView.let {
            it.layoutManager = LinearLayoutManager(it.context)
            it.adapter = orderAdapter
            DragManageAdapter(orderAdapter, ItemTouchHelper.UP.or(ItemTouchHelper.DOWN), ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT))
                .let { drag ->
                    ItemTouchHelper(drag).apply { attachToRecyclerView(it) }
                }
        }
    }

    private fun setupLiveData() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer { orderAdapter.updateList(prepareList(type)) })
            viewModel.saveNewOrderLiveData.observe(viewLifecycleOwner, EventObserver { onBackListener.onBack() })
            errorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { handleAutomaticBackupError(it, { activity?.finish() }, { activity?.finish() }) })
        }
    }

    companion object {
        private const val TYPE = "type"

        @JvmStatic
        fun newInstance(type: Int) =
            EditOrderFragment().apply {
                arguments = Bundle().apply {
                    putInt(TYPE, type)
                }
            }
    }
}
