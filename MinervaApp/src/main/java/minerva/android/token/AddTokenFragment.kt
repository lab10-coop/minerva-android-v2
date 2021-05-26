package minerva.android.token

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.View
import androidx.lifecycle.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.listener.OnBackListener
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.databinding.FragmentAddTokenBinding
import minerva.android.extension.*
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity
import minerva.android.wrapped.WrappedActivity.Companion.ADDRESS
import minerva.android.wrapped.WrappedActivity.Companion.CHAIN_ID
import minerva.android.wrapped.WrappedActivity.Companion.PRIVATE_KEY
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddTokenFragment : BaseFragment(R.layout.fragment_add_token) {

    private val viewModel: AddTokenViewModel by viewModel()
    private var addressValidatorDisposable: Disposable? = null

    private lateinit var binding: FragmentAddTokenBinding
    private lateinit var listener: AddressScannerListener
    private lateinit var showFragmentListener: ShowFragmentListener
    private lateinit var onBackListener: OnBackListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddTokenBinding.bind(view)
        showFragmentListener = (activity as WrappedActivity)
        onBackListener = (activity as WrappedActivity)
        initFragment()
        prepareLiveData()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun onResume() {
        super.onResume()
        prepareAddressListener()
    }

    override fun onPause() {
        super.onPause()
        addressValidatorDisposable?.dispose()
        showFragmentListener.setActionBarTitle(getString(R.string.manage_token))
    }

    fun setScanResult(address: String?) {
        address?.let {
            binding.tokenAddress.setText(it)
        }
    }

    private fun showTokenData(token: ERC20Token) {
        binding.apply {
            if (tokenAddress.text.toString() == token.address) {
                TransitionManager.beginDelayedTransition(root)
                tokenImage.initView(token)
                addTokenButton.isEnabled = true
                supportText.gone()
                address.apply {
                    setDataOrHide(getString(R.string.address), token.address)
                    setEllipsize(TextUtils.TruncateAt.MIDDLE)
                }
                name.setDataOrHide(getString(R.string.name), token.name)
                symbol.setDataOrHide(getString(R.string.symbol), token.symbol)
                decimals.setDataOrHide(getString(R.string.decimals), token.decimals)
                addTokenButton.setOnClickListener {
                    viewModel.addToken(token)
                }
            }
        }
    }

    private fun onLoading(isLoading: Boolean) {
        binding.loader.visibleOrGone(isLoading)
    }

    private fun prepareAddressListener() {
        binding.apply {
            addressValidatorDisposable = tokenAddress.getValidationObservable(tokenAddressLayout) {
                Validator.validateAddress(it, viewModel.isAddressValid(it), R.string.invalid_token_address)
            }.subscribeBy(
                onNext = {
                    if (it) {
                        addTokenButton.hideKeyboard()
                        viewModel.getTokenDetails(tokenAddress.text.toString())
                    } else onError()
                }
            )
        }
    }

    private fun prepareLiveData() {
        viewModel.apply {
            addressDetailsLiveData.observe(viewLifecycleOwner, Observer { token -> showTokenData(token) })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { isLoading -> onLoading(isLoading) })
            tokenAddedLiveData.observe(viewLifecycleOwner, EventObserver { onBackListener.onBack() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                MinervaFlashbar.showError(requireActivity(), it)
                onError()
            })
        }
    }

    private fun onError() {
        binding.apply {
            tokenImage.clear()
            addTokenButton.isEnabled = false
            tokenAddressLayout.error = getString(R.string.invalid_token_address)
            tokenAddressLayout.setErrorIconDrawable(NO_ICON)
            supportText.visible()
            name.gone()
            address.gone()
            symbol.gone()
            decimals.gone()
        }
    }

    private fun initFragment() {
        arguments?.let { bundle ->
            viewModel.setAccountData(
                bundle.getString(PRIVATE_KEY, String.Empty),
                bundle.getInt(CHAIN_ID, Int.InvalidId),
                bundle.getString(ADDRESS, String.Empty)
            )
        }
        binding.apply {
            with(tokenAddress) {
                onRightDrawableClicked {
                    if (text.toString().isEmpty())
                        listener.showScanner(AddressScannerFragment.newInstance())
                }
            }
            supportText.text = String.format(getString(R.string.minerva_support), viewModel.getNetworkName())
        }
    }

    companion object {
        fun newInstance(privateKey: String, chainId: Int, address: String) = AddTokenFragment().apply {
            arguments = Bundle().apply {
                putString(PRIVATE_KEY, privateKey)
                putInt(CHAIN_ID, chainId)
                putString(ADDRESS, address)
            }
        }
    }
}