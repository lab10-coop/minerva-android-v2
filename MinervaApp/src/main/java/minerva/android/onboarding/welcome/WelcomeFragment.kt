package minerva.android.onboarding.welcome

import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
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

    private val onPageChangeCallback = createOnPageChangeCallBack()
    private var currentPage: Int = MIN_POSITION
    private lateinit var carouselTimer: Timer
    private val carouselHandler = Handler()
    private val carouselUpdate = Runnable {
        var smoothScroll = true

        if (currentPage == ITEM_COUNT) {
            currentPage = MIN_POSITION
            smoothScroll = false
        }
        binding.onboardingViewPager.setCurrentItem(currentPage++, smoothScroll)
    }

    override fun onResume() {
        super.onResume()
        binding.onboardingViewPager.registerOnPageChangeCallback(onPageChangeCallback)
        setupAutomaticCarousel()
    }

    override fun onPause() {
        super.onPause()
        binding.onboardingViewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
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
        onboardingViewPager.adapter = OnBoardingAdapter()
        TabLayoutMediator(tabs, onboardingViewPager) { _, _ -> }.attach()
    }

    private fun setupAutomaticCarousel() {
        carouselTimer = Timer()
        carouselTimer.schedule(object : TimerTask() {
            override fun run() {
                carouselHandler.post(carouselUpdate)
            }
        }, DELAY, CAROUSEL_PERIOD)
    }

    private fun createOnPageChangeCallBack(): ViewPager2.OnPageChangeCallback {
        return object : ViewPager2.OnPageChangeCallback() {
            var state = ViewPager2.SCROLL_STATE_IDLE

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                this.state = state
            }

            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) = with(binding.onboardingViewPager) {
                super.onPageScrolled(position, offset, offsetPixels)
                if (isDraggingStateWithNoOffset(state, offset, offsetPixels)) {
                    getCurrentPositionForMarginalItems(position)?.let { newPosition ->
                        setCurrentItem(newPosition, false)
                        currentPage = newPosition
                    } ?: Unit
                } else {
                    currentPage = position
                }
            }
        }
    }

    private fun getCurrentPositionForMarginalItems(position: Int): Int? =
        when (position) {
            MAX_POSITION -> MIN_POSITION
            MIN_POSITION -> MAX_POSITION
            else -> null
        }

    private fun isDraggingStateWithNoOffset(state: Int, offset: Float, offsetPixels: Int) =
        state == SCROLL_STATE_DRAGGING && offset == ZERO_OFFSET && offsetPixels == ZERO_PIXELS_OFFSET

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeFragment()

        private const val DELAY = 0L
        private const val CAROUSEL_PERIOD = 1000L
        private const val ITEM_COUNT = 4
        private const val MAX_POSITION = 3
        private const val MIN_POSITION = 0
        private const val ZERO_OFFSET = 0F
        private const val ZERO_PIXELS_OFFSET = 0
    }
}
