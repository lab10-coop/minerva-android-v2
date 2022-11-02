package minerva.android.services

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import minerva.android.R
import minerva.android.databinding.ServicesFragmentLayoutBinding
import minerva.android.extension.onTabSelected
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.services.adapter.ServicesPagerAdapter
import minerva.android.services.connections.ConnectionsFragment
import minerva.android.services.dapps.DappsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : BaseFragment(R.layout.services_fragment_layout) {

    private lateinit var binding: ServicesFragmentLayoutBinding
    var currentFragment: Fragment = getFragment(DAPPS_POSITION)
    private val viewModel: ServicesViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding =  ServicesFragmentLayoutBinding.bind(view)
        setupLiveData()
        setupViewPager()
    }

    private fun setupLiveData() {
        viewModel.apply {
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
        }
    }

    private fun setupViewPager() {
        with(binding) {
            servicesViewPager.apply {
                adapter = ServicesPagerAdapter(this@ServicesFragment, ::getFragment)
                setCurrentItem(FIRST_PAGE, false)

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        servicesTabs.selectTab(servicesTabs.getTabAt(position % CARD_NUMBER))
                    }
                })

                addTab(servicesTabs, R.string.dapps_label)
                addTab(servicesTabs, R.string.connections_label)

                servicesTabs.onTabSelected {
                    currentFragment = getFragment(it)
                    requireActivity().invalidateOptionsMenu()
                    setCurrentItem(it, true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
    }

    private fun getFragment(position: Int) =
        when (position) {
            DAPPS_POSITION -> DappsFragment.newInstance()
            else -> ConnectionsFragment.newInstance()
        }

    private fun addTab(tabLayout: TabLayout, titleRes: Int) {
        tabLayout.addTab(tabLayout.newTab().setText(getString(titleRes)))
    }

    companion object {
        const val DAPPS_POSITION = 0
        const val CARD_NUMBER = 2
        private const val FIRST_PAGE = 0
        @JvmStatic
        fun newInstance() = ServicesFragment()
    }
}