package minerva.android.token.ramp

import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.GridLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentRampBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.token.ramp.adapter.AccountSpinnerAdapter
import minerva.android.token.ramp.adapter.RampCryptoAdapter
import minerva.android.token.ramp.adapter.RampCryptoViewHolder.Companion.DEFAULT_RAMP_CRYPTO_POSITION
import minerva.android.token.ramp.listener.OnRampCryptoChangedListener
import minerva.android.token.ramp.model.RampCrypto
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class RampFragment : BaseFragment(R.layout.fragment_ramp), OnRampCryptoChangedListener {

    private lateinit var binding: FragmentRampBinding
    private val viewModel: RampViewModel by viewModel()
    private val cryptoAdapter by lazy { RampCryptoAdapter(crypto, this) }

    //TODO klop how to store crypto ramp?
    private val crypto = listOf(
            RampCrypto(ETH_MAIN, "ETH", R.drawable.ic_ethereum_token),
            RampCrypto(ETH_MAIN, "DAI", R.drawable.ic_dai_token),
            RampCrypto(XDAI, "xDAI", R.drawable.ic_xdai_token),
            RampCrypto(ETH_MAIN, "USDC", R.drawable.ic_usdc_token)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRampBinding.bind(view)
        initializeFragment(view)
        showCurrentAccounts(viewModel.getValidAccounts(crypto[DEFAULT_RAMP_CRYPTO_POSITION].chainId))
    }

    override fun onRampCryptoChanged(chainId: Int) = showCurrentAccounts(viewModel.getValidAccounts(chainId))

    private fun initializeFragment(view: View) {
        binding.apply {
            cryptoRecycler.apply {
                layoutManager = GridLayoutManager(view.context, RAMP_CRYPTO_COLUMNS)
                adapter = cryptoAdapter
            }
            continueButton.setOnClickListener { openRampScreen() }
            createNewAccount.setOnClickListener { createNewAccount() }

            viewModel.apply {
                createAccountLiveData.observe(viewLifecycleOwner, EventObserver { showCurrentAccounts(viewModel.currentAccounts) })
                loadingLiveData.observe(viewLifecycleOwner, EventObserver { showProgressBar(it) })
                errorLiveData.observe(viewLifecycleOwner, EventObserver { MinervaFlashbar.showError(requireActivity(), it) })
            }
        }
    }

    private fun openRampScreen() {
        Log.e("klop", "Going to RAMP screen with account: ${viewModel.getCurrentAccount().name}")
    }

    private fun createNewAccount() = viewModel.createNewAccount(viewModel.currentChainId)

    private fun showCurrentAccounts(accounts: List<Account>) {
        if (crypto.isNotEmpty()) {
            binding.apply {
                TransitionManager.beginDelayedTransition(container)
                noAccountLayout.visibleOrGone(accounts.isEmpty())
                accounts.isNotEmpty().let {
                    continueButton.isEnabled = it
                    cryptoSpinner.isEnabled = it
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
                    adapter = AccountSpinnerAdapter(context, R.layout.spinner_network, accounts + Account(Int.InvalidId))
                            .apply { setDropDownViewResource(R.layout.spinner_token) }
                    setSelection(viewModel.spinnerPosition, false)
                    setPopupBackgroundResource(R.drawable.rounded_white_background)
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position == accounts.size) createNewAccount()
                            viewModel.spinnerPosition = position
                        }

                        override fun onNothingSelected(adapterView: AdapterView<*>?) = setSelection(viewModel.spinnerPosition, true)
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
    }
}