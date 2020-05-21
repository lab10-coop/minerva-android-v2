package minerva.android.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.services.adapter.ServicesAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : Fragment() {

    private val servicesAdapter = ServicesAdapter()
    private val viewModel: ServicesViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        prepareWalletConfigObserver()
    }

    private fun prepareWalletConfigObserver() {
        viewModel.walletConfigLiveData.observe(viewLifecycleOwner, Observer {
            noDataMessage.visibleOrGone(it.services.isEmpty())
            servicesAdapter.updateList(it.services)
        })
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = servicesAdapter
        }
    }
}
