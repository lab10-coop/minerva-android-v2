package minerva.android.identities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.identities.adapter.IdentityAdapter
import minerva.android.walletmanager.model.Identity

class IdentitiesFragment : Fragment() {

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

        prepareMockedData()
    }

    //TODO Move mocked data to WalletManager
    private fun prepareMockedData() {
        val map1: LinkedHashMap<String, String> = linkedMapOf(
            "Name" to "Tom Johnson",
            "Email" to "tj@mail.com",
            "Date of Brith" to "12.09.1991",
            "Some Key" to "Some value",
            "Some Key 2" to "Some value",
            "Some Key 3" to "Some value",
            "Some Key 4" to "Some value"
        )

        val map2: LinkedHashMap<String, String> = linkedMapOf(
            "Name" to "James Adams",
            "Email" to "ja@email.com",
            "Date of Brith" to "13.03.1974"
        )

        val map3: LinkedHashMap<String, String> = linkedMapOf(
            "Name" to "Jannie Cort",
            "Email" to "jc@emailcom"
        )

        val map4: LinkedHashMap<String, String> = linkedMapOf(
            "Name" to "Michael Knox"
        )

        val map5: LinkedHashMap<String, String> = linkedMapOf()

        val list = listOf(
            Identity("0", "", "", "Citizen", map1, false),
            Identity("1", "", "", "Work", map2),
            Identity("2", "", "", "Judo", map3),
            Identity("3", "", "", "Car", map4),
            Identity("4", "", "", "Family", map5),
            Identity("0", "", "", "Citizen", map1),
            Identity("1", "", "", "Work", map2),
            Identity("2", "", "", "Judo", map3),
            Identity("3", "", "", "Car", map4),
            Identity("4", "", "", "Family", map5),
            Identity("0", "", "", "Citizen", map1),
            Identity("1", "", "", "Work", map2),
            Identity("2", "", "", "Judo", map3),
            Identity("3", "", "", "Car", map4),
            Identity("4", "", "", "Family", map5)
        )


        identityAdapter.updateList(list)
    }
}
