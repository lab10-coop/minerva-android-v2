package minerva.android.identities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.identities_fragment_layout.*
import minerva.android.R
import minerva.android.extension.onTabSelected
import minerva.android.identities.adapter.IdentitiesPagerAdapter
import minerva.android.identities.contacts.ContactsFragment
import minerva.android.identities.credentials.CredentialsFragment
import minerva.android.identities.myIdentities.MyIdentitiesFragment

class IdentitiesFragment : Fragment() {

    var currentFragment: Fragment = getFragment(MY_IDENTITIES_POSITION)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.identities_fragment_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        identityViewPager.apply {
            adapter = IdentitiesPagerAdapter(this@IdentitiesFragment, ::getFragment)
            setCurrentItem(START_VIEW_PAGER_POSITION, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    identityTabs.selectTab(identityTabs.getTabAt(position % CARD_NUMBER))
                }
            })

            addTab(identityTabs, R.string.my_identities_label)
            addTab(identityTabs, R.string.contacts_label)
            addTab(identityTabs, R.string.credentials_label)

            identityTabs.onTabSelected {
                currentFragment = getFragment(it)
                activity?.invalidateOptionsMenu()
                setCurrentItem(it + START_VIEW_PAGER_POSITION, true)
            }
        }
    }

    private fun getFragment(position: Int) =
        when (position % CARD_NUMBER) {
            MY_IDENTITIES_POSITION -> MyIdentitiesFragment.newInstance()
            CONTACTS_POSITION -> ContactsFragment.newInstance()
            else -> CredentialsFragment.newInstance()
        }

    private fun addTab(tabLayout: TabLayout, titleRes: Int) {
        tabLayout.addTab(tabLayout.newTab().setText(getString(titleRes)))
    }

    companion object {
        const val MY_IDENTITIES_POSITION = 0
        const val CONTACTS_POSITION = 1
        const val CARD_NUMBER = 3
        private const val START_VIEW_PAGER_POSITION = 150

    }
}