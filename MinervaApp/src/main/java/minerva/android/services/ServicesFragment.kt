package minerva.android.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_services.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.services.adapter.ServicesAdapter
import minerva.android.walletmanager.model.WalletConfig
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : Fragment() {

    private val servicesAdapter = ServicesAdapter()
    private val viewModel: ServicesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_services, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        prepareWalletConfigObserver()
    }

    private fun prepareWalletConfigObserver() {
        viewModel.walletConfigLiveData.observe(this, Observer { handleServicesList(it) })
    }

    private fun handleServicesList(it: WalletConfig) {
        if (it.services.isEmpty()) {
            hideServices()
        } else {
            showServices(it)
        }
    }

    private fun hideServices() {
        noDataMessage.visible()
        recyclerView.gone()
    }

    private fun showServices(it: WalletConfig) {
        noDataMessage.gone()
        recyclerView.visible()
        servicesAdapter.updateList(it.services)
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = servicesAdapter
        }
    }
}
