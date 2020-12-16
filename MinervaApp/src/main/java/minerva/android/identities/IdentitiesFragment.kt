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
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class IdentitiesFragment : BaseFragment() {

    var currentFragment: Fragment = getFragment(MY_IDENTITIES_POSITION)
    private val viewModel: MinervaPrimitivesViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.identities_fragment_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLiveData()
        setupViewPager()
    }

    private fun setupLiveData() {
        viewModel.apply {
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
        }
    }

    private fun setupViewPager() {
        transaction_view_pager.apply {
            adapter = IdentitiesPagerAdapter(this@IdentitiesFragment, ::getFragment)
            setCurrentItem(FIRST_PAGE, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    identityTabs.selectTab(identityTabs.getTabAt(position % CARD_NUMBER))
                }
            })

            addTab(identityTabs, R.string.my_identities_label)
            addTab(identityTabs, R.string.credentials_label)
            addTab(identityTabs, R.string.contacts_label)

            identityTabs.onTabSelected {
                currentFragment = getFragment(it)
                activity?.invalidateOptionsMenu()
                setCurrentItem(it, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
    }

    private fun getFragment(position: Int) =
        when (position % CARD_NUMBER) {
            MY_IDENTITIES_POSITION -> MyIdentitiesFragment.newInstance()
            CREDENTIALS_POSITION -> CredentialsFragment.newInstance()
            else -> ContactsFragment.newInstance()
        }

    private fun addTab(tabLayout: TabLayout, titleRes: Int) {
        tabLayout.addTab(tabLayout.newTab().setText(getString(titleRes)))
    }

    companion object {
        const val MY_IDENTITIES_POSITION = 0
        const val CREDENTIALS_POSITION = 1
        const val CARD_NUMBER = 3
        private const val FIRST_PAGE = 0
    }
}