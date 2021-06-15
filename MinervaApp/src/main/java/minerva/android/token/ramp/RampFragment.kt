package minerva.android.token.ramp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.AdapterView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.databinding.FragmentRampBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.token.ramp.RampViewModel.Companion.rampCrypto
import minerva.android.token.ramp.adapter.AccountSpinnerAdapter
import minerva.android.token.ramp.adapter.RampCryptoAdapter
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class RampFragment : BaseFragment(R.layout.fragment_ramp) {

    private lateinit var binding: FragmentRampBinding
    private val viewModel: RampViewModel by viewModel()
    private val cryptoAdapter by lazy {
        RampCryptoAdapter(rampCrypto) { chainId, symbol ->
            viewModel.setAccountSpinnerDefaultPosition()
            showCurrentAccounts(viewModel.getValidAccountsAndLimit(chainId))
            viewModel.currentSymbol = symbol
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRampBinding.bind(view)
        initializeFragment()
        showCurrentAccounts(viewModel.getValidAccountsAndLimit(rampCrypto[DEFAULT_RAMP_CRYPTO_POSITION].chainId))
    }

    private fun initializeFragment() {
        binding.apply {
            tokensViewPager.apply {
                adapter = cryptoAdapter
                TabLayoutMediator(tabLayout, tokensViewPager) { _, _ -> }.attach()
                tokensViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        tokensViewPager.post {
                            cryptoAdapter.notifyData()
                        }
                    }
                })
            }
            continueButton.setOnClickListener { openRampScreen() }
            createNewAccount.setOnClickListener { createNewAccount() }
            viewModel.apply {
                createAccountLiveData.observe(
                    viewLifecycleOwner,
                    EventObserver { showCurrentAccounts(viewModel.getValidAccountsAndLimit()) })
                loadingLiveData.observe(viewLifecycleOwner, EventObserver { isLoading -> showProgressBar(isLoading) })
                errorLiveData.observe(
                    viewLifecycleOwner,
                    EventObserver { error -> MinervaFlashbar.showError(requireActivity(), error) })
            }
        }
    }

    private fun openRampScreen() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.Builder()
                .scheme(SCHEME)
                .authority(BuildConfig.RAMP_API_URL)
                .appendQueryParameter(SWAP_ASSET, viewModel.currentSymbol)
                .appendQueryParameter(USER_ADDRESS, viewModel.getCurrentCheckSumAddress())
                .appendQueryParameter(HOST_API_KEY, BuildConfig.RAMP_API_KEY)
                .appendQueryParameter(HOST_APP_NAME, getString(R.string.app_name))
                .appendQueryParameter(HOST_LOGO_URL, MINERVA_LOGO_URL)
                .build()
        }
        startActivity(intent)
    }

    private fun createNewAccount() = viewModel.createNewAccount()

    private fun showCurrentAccounts(accountsInfo: Pair<Int, List<Account>>) {
        if (rampCrypto.isNotEmpty()) {
            binding.apply {
                TransitionManager.beginDelayedTransition(container)
                noAccountLayout.visibleOrGone(accountsInfo.second.isEmpty())
                accountsInfo.second.isNotEmpty().let { isNotEmpty ->
                    continueButton.isEnabled = isNotEmpty
                    cryptoSpinner.visibleOrGone(isNotEmpty)
                    if (isNotEmpty) updateSpinner(accountsInfo.first, accountsInfo.second)
                }
            }
        }
    }

    private fun updateSpinner(numberOfAccountsToUse: Int, accounts: List<Account>) =
        binding.cryptoSpinner.apply {
            setBackgroundResource(R.drawable.rounded_spinner_background)
            adapter = AccountSpinnerAdapter(
                context,
                R.layout.spinner_network,
                accounts + Account(Int.InvalidId),
                numberOfAccountsToUse
            ).apply { setDropDownViewResource(R.layout.spinner_token) }
            setSelection(viewModel.spinnerPosition, false)
            setPopupBackgroundResource(R.drawable.rounded_white_background)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when {
                        position == accounts.size && accounts.size < numberOfAccountsToUse -> {
                            createNewAccount()
                            viewModel.spinnerPosition = position
                        }
                        position == accounts.size && accounts.size >= numberOfAccountsToUse -> setSelection(viewModel.spinnerPosition)
                        else -> viewModel.spinnerPosition = position
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) =
                    setSelection(viewModel.spinnerPosition, true)
            }
        }

    private fun showProgressBar(value: Boolean) {
        binding.apply {
            progressBar.visibleOrGone(value)
            viewModel.currentAccounts.let { accounts ->
                noAccountLayout.visibleOrGone(!value && accounts.isEmpty())
                cryptoSpinner.visibleOrGone(!value && accounts.isNotEmpty())
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RampFragment()
        const val DEFAULT_RAMP_CRYPTO_POSITION = 0
        private const val SCHEME = "https"
        private const val SWAP_ASSET = "swapAsset"
        private const val USER_ADDRESS = "userAddress"
        private const val HOST_API_KEY = "hostApiKey"
        private const val HOST_APP_NAME = "hostAppName"
        private const val HOST_LOGO_URL = "hostLogoUrl"
        private const val MINERVA_LOGO_URL = "https://minerva.digital/i/minerva-owl.svg"
    }
}