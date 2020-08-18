package minerva.android.identities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.identities_fragment_layout.*
import minerva.android.R
import minerva.android.identities.adapter.IdentitiesPagerAdapter

class IdentitiesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.identities_fragment_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identityViewPager.adapter = IdentitiesPagerAdapter(this)
        TabLayoutMediator(identityTabs, identityViewPager) { tab, position ->
            when (position) {
                MY_IDENTITIES_POSITION -> tab.text = getString(R.string.my_identities_label)
                CONTACTS_POSITION -> tab.text = getString(R.string.contacts_label)
                CREDENTIALS_POSITION -> tab.text = getString(R.string.credentials_label)
            }
        }.attach()
    }

    companion object {
        const val MY_IDENTITIES_POSITION = 0
        const val CONTACTS_POSITION = 1
        const val CREDENTIALS_POSITION = 2
    }
}