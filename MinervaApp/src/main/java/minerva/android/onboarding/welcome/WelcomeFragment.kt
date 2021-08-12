package minerva.android.onboarding.welcome

import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import minerva.android.R
import minerva.android.databinding.FragmentWelcomeBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.base.BaseOnBoardingFragment
import minerva.android.onboarding.create.CreateWalletViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class WelcomeFragment : BaseOnBoardingFragment(R.layout.fragment_welcome) {

    private lateinit var binding: FragmentWelcomeBinding
    private val viewModel: CreateWalletViewModel by viewModel()

    private var currentPage: Int = MIN_POSITION
    private lateinit var carouselTimer: Timer
    private val carouselHandler = Handler()
    private val carouselUpdate by lazy {
        Runnable {
            var smoothScroll = true

            if (currentPage == ITEM_COUNT) {
                currentPage = MIN_POSITION
                smoothScroll = false
            }
            binding.onboardingViewPager.setCurrentItem(currentPage++, smoothScroll)
        }
    }

    override fun onResume() {
        super.onResume()
        listener.updateActionBar()
        setupAutomaticCarousel()
    }

    override fun onPause() {
        super.onPause()
        carouselHandler.removeCallbacks(carouselUpdate)
        carouselTimer.cancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWelcomeBinding.bind(view)
        handleCreateWalletButton()
        handleRestoreWalletButton()
        setupViewPager()
        setupTermsLink()
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.apply {
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            createWalletLiveData.observe(viewLifecycleOwner, EventObserver { listener.showMainActivity() })
        }
    }

    private fun hideLoader() = with(binding) {
        createWalletButton.visible()
        createWalletProgressBar.invisible()
        restoreWalletButton.isEnabled = true
    }

    private fun showLoader() = with(binding) {
        createWalletProgressBar.visible()
        createWalletButton.invisible()
        restoreWalletButton.isEnabled = false
    }

    private fun handleCreateWalletButton() {
        binding.createWalletButton.setOnClickListener {
            viewModel.createWalletConfig()
        }
    }

    private fun handleRestoreWalletButton() {
        binding.restoreWalletButton.setOnClickListener {
            listener.showRestoreWalletFragment()
        }
    }

    private fun setupTermsLink() {
        binding.termsOfService.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupViewPager() = with(binding) {
        onboardingViewPager.apply {
            adapter = OnBoardingAdapter()
            isUserInputEnabled = false
            TabLayoutMediator(tabs, this) { _, _ -> }.attach()
        }
    }

    private fun setupAutomaticCarousel() {
        carouselTimer = Timer()
        carouselTimer.schedule(object : TimerTask() {
            override fun run() {
                carouselHandler.post(carouselUpdate)
            }
        }, DELAY, CAROUSEL_PERIOD)
    }

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeFragment()

        private const val DELAY = 0L
        private const val CAROUSEL_PERIOD = 2000L
        private const val ITEM_COUNT = 4
        private const val MIN_POSITION = 0
    }
}
