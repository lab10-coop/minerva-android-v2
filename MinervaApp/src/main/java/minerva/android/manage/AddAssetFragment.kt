package minerva.android.manage

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.databinding.FragmentAddAssetBinding
import minerva.android.databinding.FragmentManageAssetsBinding
import minerva.android.extension.*
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.Asset
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddAssetFragment : BaseFragment(R.layout.fragment_add_asset) {

    private val viewModel: AddAssetViewModel by viewModel()
    private var addressValidatorDisposable: Disposable? = null

    private lateinit var binding: FragmentAddAssetBinding
    private lateinit var listener: AddressScannerListener
    private lateinit var showFragmentListener: ShowFragmentListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddAssetBinding.bind(view)
        showFragmentListener = (activity as WrappedActivity)
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
        showFragmentListener.setActionBarTitle(getString(R.string.manage_assets))
    }

    private fun showAssetData(asset: Asset) {
        binding.apply {
            val isCorrectData = asset.name != String.Empty
            TransitionManager.beginDelayedTransition(this.root)
            prepareAssetLogo(isCorrectData)
            addTokenButton.visibleOrGone(isCorrectData)
            supportText.visibleOrGone(!isCorrectData)
            address.apply {
                setDataOrHide(getString(R.string.address), asset.address)
                setEllipsize(TextUtils.TruncateAt.MIDDLE)
            }
            name.setDataOrHide(getString(R.string.name), asset.name)
            symbol.setDataOrHide(getString(R.string.symbol), asset.shortName)
            decimals.setDataOrHide(getString(R.string.decimals), asset.decimals)

            if (!isCorrectData) {
                tokenAddressLayout.error = getString(R.string.invalid_asset_address)
                tokenAddressLayout.setErrorIconDrawable(NO_ICON)
            }
        }
    }

    private fun prepareAssetLogo(assetData: Boolean) {
        //TODO implement generating and adding asset image
        binding.apply {
            assetImage.setImageResource(R.drawable.ic_default_token)
            assetImageCamera.visibleOrGone(assetData)
            assetImageContainer.isEnabled = assetData
        }
    }

    private fun onLoading(isLoading: Boolean) {
        binding.apply {
            loader.visibleOrGone(isLoading)
            addTokenButton.visibleOrGone(!isLoading)
        }
    }

    private fun prepareAddressListener() {
        binding.apply {
            addressValidatorDisposable = tokenAddress.getValidationObservable(tokenAddressLayout) {
                Validator.validateAddress(it, viewModel.isAddressValid(it), R.string.invalid_asset_address)
            }.subscribeBy(
                onNext = {
                    //TODO getting asset details is only mock. Will be implemented in one of the next task
                    if (it) {
                        addTokenButton.hideKeyboard()
                        viewModel.getAssetDetails(tokenAddress.text.toString())
                    }
                }
            )
            assetImageContainer.setOnClickListener {
                Toast.makeText(context, "Assets logo editing will be enabled soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareLiveData() {
        viewModel.apply {
            addressDetailsLiveData.observe(viewLifecycleOwner, Observer { showAssetData(it) })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { onLoading(it) })
            errorLiveData.observe(viewLifecycleOwner, EventObserver { MinervaFlashbar.showError(requireActivity(), it) })
        }
    }

    private fun initFragment() {
        binding.apply {
            addTokenButton.setOnClickListener {
                Toast.makeText(requireContext(), "Add token N O W ! - will be available soon", Toast.LENGTH_SHORT).show()
            }
            tokenAddress.onRightDrawableClicked {
                listener.showScanner()
            }
        }
    }

    companion object {
        fun newInstance() = AddAssetFragment()
    }
}