package minerva.android.identities

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.identities.adapter.IdentityAdapter
import minerva.android.walletmanager.model.Identity
import org.koin.androidx.viewmodel.ext.android.viewModel

class IdentitiesFragment : Fragment() {

    private val viewModel: IdentitiesViewModel by viewModel()
    private val identityAdapter = IdentityAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.recycler_view_layout, container, false)


    //TODO BaseFragment?
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = identityAdapter
        }

        viewModel.walletConfigLiveData().observe(this, Observer { identityAdapter.updateList(it.identities) })
    }
}
