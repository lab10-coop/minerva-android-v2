package minerva.android.manage

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
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
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.Token
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity
import minerva.android.wrapped.WrappedActivity.Companion.NETWORK
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
        prepareAddressListener()
        prepareLiveData()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun onPause() {
        super.onPause()
        addressValidatorDisposable?.dispose()
        showFragmentListener.setActionBarTitle(getString(R.string.manage_token))
    }

    private fun showTokenData(token: Token) {
        binding.apply {
            TransitionManager.beginDelayedTransition(this.root)
            prepareTokenLogo(true)
            addTokenButton.visible()
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

    private fun prepareTokenLogo(tokenData: Boolean) {
        //TODO implement generating and adding token image
        binding.apply {
            tokenImage.setImageResource(R.drawable.ic_default_token)
            tokenImageCamera.visibleOrGone(tokenData)
            tokenImageContainer.isEnabled = tokenData
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
                        viewModel.getTokenDetails(
                            tokenAddress.text.toString()
                        )
                    } else onError()
                }
            )
            tokenImageContainer.setOnClickListener {
                Toast.makeText(context, "Tokens logo editing will be enabled soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareLiveData() {
        viewModel.apply {
            addressDetailsLiveData.observe(viewLifecycleOwner, Observer { showTokenData(it) })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { onLoading(it) })
            tokenAddedLiveData.observe(viewLifecycleOwner, EventObserver { onBackListener.onBack() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                MinervaFlashbar.showError(requireActivity(), it)
                onError()
            })
        }
    }

    private fun onError() {
        prepareTokenLogo(false)
        binding.apply {
            addTokenButton.gone()
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
            viewModel.initViewModel(
                bundle.getString(PRIVATE_KEY, String.Empty),
                bundle.getString(NETWORK, String.Empty)
            )
        }
        binding.tokenAddress.onRightDrawableClicked {
            listener.showScanner()
        }
    }

    companion object {
        fun newInstance(privateKey: String, network: String) = AddTokenFragment().apply {
            arguments = Bundle().apply {
                putString(PRIVATE_KEY, privateKey)
                putString(NETWORK, network)
            }
        }
    }
}