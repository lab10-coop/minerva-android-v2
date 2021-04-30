package minerva.android.token.ramp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.GridLayoutManager
import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.databinding.FragmentRampBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.token.ramp.adapter.AccountSpinnerAdapter
import minerva.android.token.ramp.adapter.RampCryptoAdapter
import minerva.android.token.ramp.adapter.RampCryptoViewHolder.Companion.DEFAULT_RAMP_CRYPTO_POSITION
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class RampFragment : BaseFragment(R.layout.fragment_ramp) {

    private lateinit var binding: FragmentRampBinding
    private val viewModel: RampViewModel by viewModel()
    private val cryptoAdapter by lazy { RampCryptoAdapter(viewModel.rampCrypto) { showCurrentAccounts(viewModel.getValidAccounts(it)) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRampBinding.bind(view)
        initializeFragment(view)
        showCurrentAccounts(viewModel.getValidAccounts(viewModel.rampCrypto[DEFAULT_RAMP_CRYPTO_POSITION].chainId))
    }

    private fun initializeFragment(view: View) {
        binding.apply {
            cryptoRecycler.apply {
                layoutManager = GridLayoutManager(view.context, RAMP_CRYPTO_COLUMNS)
                adapter = cryptoAdapter
            }
            continueButton.setOnClickListener { openRampScreen() }
            createNewAccount.setOnClickListener { createNewAccount() }

            viewModel.apply {
                createAccountLiveData.observe(
                    viewLifecycleOwner,
                    EventObserver { showCurrentAccounts(viewModel.currentAccounts) })
                loadingLiveData.observe(viewLifecycleOwner, EventObserver { showProgressBar(it) })
                errorLiveData.observe(
                    viewLifecycleOwner,
                    EventObserver { MinervaFlashbar.showError(requireActivity(), it) })
            }
        }
    }

    private fun openRampScreen() {
        val intent = Intent(Intent.ACTION_VIEW).apply {

            Log.e("klop", "Current address: ${viewModel.getCurrentAccount().address}")

            data = Uri.Builder()
                .scheme(SCHEME)
                .authority(BuildConfig.RAMP_API_URL)
                .appendQueryParameter(SWAP_ASSET, viewModel.rampCrypto[cryptoAdapter.getCryptoPosition()].symbol)
                .appendQueryParameter(USER_ADDRESS, viewModel.getCurrentAccount().address)
                .appendQueryParameter(HOST_API_KEY, BuildConfig.RAMP_API_KEY)
                .appendQueryParameter(HOST_APP_NAME, getString(R.string.app_name))
                .appendQueryParameter(HOST_LOGO_URL, MINERVA_LOGO_URL)
                .build()
        }
        startActivity(intent)
    }

    private fun createNewAccount() = viewModel.createNewAccount()

    private fun showCurrentAccounts(accounts: List<Account>) {
        if (viewModel.rampCrypto.isNotEmpty()) {
            binding.apply {
                TransitionManager.beginDelayedTransition(container)
                noAccountLayout.visibleOrGone(accounts.isEmpty())
                accounts.isNotEmpty().let {
                    continueButton.isEnabled = it
                    cryptoSpinner.visibleOrGone(it)
                    if (it) updateSpinner(accounts)
                }
            }
        }
    }

    private fun updateSpinner(accounts: List<Account>) =
        binding.apply {
            cryptoSpinner.apply {
                setBackgroundResource(R.drawable.rounded_spinner_background)
                adapter = AccountSpinnerAdapter(
                    context,
                    R.layout.spinner_network,
                    accounts + Account(Int.InvalidId)
                )
                    .apply { setDropDownViewResource(R.layout.spinner_token) }
                setSelection(viewModel.spinnerPosition, false)
                setPopupBackgroundResource(R.drawable.rounded_white_background)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position == accounts.size) createNewAccount()
                        viewModel.spinnerPosition = position
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) =
                        setSelection(viewModel.spinnerPosition, true)
                }
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
        private const val RAMP_CRYPTO_COLUMNS = 2
        private const val SCHEME = "https"
        private const val SWAP_ASSET = "swapAsset"
        private const val USER_ADDRESS = "userAddress"
        private const val HOST_API_KEY = "hostApiKey"
        private const val HOST_APP_NAME = "hostAppName"
        private const val HOST_LOGO_URL = "hostLogoUrl"
        private const val MINERVA_LOGO_URL = "https://minerva.digital/i/minerva-owl.svg"
    }
}