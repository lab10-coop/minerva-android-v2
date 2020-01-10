package minerva.android.values

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.values.adapter.ValueAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class ValuesFragment : Fragment() {

    private val viewModel: ValuesViewModel by viewModel()
    private val valueAdapter = ValueAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView(view)
        setupLiveData()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupRecycleView(view: View) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = valueAdapter
        }
    }

    private fun setupLiveData() {
        viewModel.walletConfigLiveData.observe(this, Observer { valueAdapter.updateList(it.values) })
    }
}
