package minerva.android.accounts.akm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_safe_account_settings.*
import minerva.android.R
import minerva.android.accounts.adapter.OwnerAdapter
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.listener.OnOwnerRemovedListener
import minerva.android.extension.getValidationObservable
import minerva.android.extension.onRightDrawableClicked
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.token.AddressScannerFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SafeAccountSettingsFragment : Fragment(), OnOwnerRemovedListener {

    private val viewModel: SafeAccountSettingsViewModel by viewModel()
    private val ownerAdapter = OwnerAdapter(this)
    private var safeAccountDisposable: Disposable? = null
    private lateinit var listener: AddressScannerListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_safe_account_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFragment()
    }

    override fun onResume() {
        super.onResume()
        prepareTextValidator()
    }

    override fun onPause() {
        super.onPause()
        safeAccountDisposable?.dispose()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    private fun initializeFragment() {
        setupRecycleView()
        prepareAddButton()
        setAddressScannerListener()
        activity?.window?.setSoftInputMode(SOFT_INPUT_ADJUST_PAN)
        viewModel.apply {
            ownersLiveData.observe(viewLifecycleOwner, Observer { ownerAdapter.updateList(it) })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                val message = it.message ?: getString(R.string.unexpected_error)
                MinervaFlashbar.show(
                    requireActivity(),
                    getString(R.string.error_header),
                    message
                )
                Timber.e(it)
            })
            arguments?.let { loadAccount(it.getInt(INDEX)) }
        }
    }

    fun setScanResult(result: String?) {
        result?.let {
            newOwner.setText(it)
        }
    }

    private fun prepareAddButton() {
        addOwnerButton.setOnClickListener {
            viewModel.addOwner(newOwner.text.toString())
            newOwner.clearFocus()
            safeAccountDisposable?.dispose()
            addOwnerButton.isEnabled = false
            newOwner.text?.clear()
            prepareTextValidator()
        }
    }

    override fun onOwnerRemoved(removeAddress: String) {
        AlertDialogHandler.showRemoveDialog(
            requireContext(),
            getString(R.string.remove_owner),
            getString(
                R.string.remove_safe_account_dialog_message,
                removeAddress,
                viewModel.accountName
            )
        ) { viewModel.removeOwner(removeAddress) }
    }


    private fun prepareTextValidator() {
        safeAccountDisposable = newOwner.getValidationObservable(ownerAddressInputLayout) {
            Validator.validateAddress(it, viewModel.isAddressValid(it), R.string.invalid_account_address)
        }
            .subscribeBy(
                onNext = { addOwnerButton.isEnabled = it },
                onError = { addOwnerButton.isEnabled = false }
            )
    }

    private fun setupRecycleView() {
        owners.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ownerAdapter
        }
    }

    private fun setAddressScannerListener() {
        newOwner.onRightDrawableClicked { listener.showScanner(AddressScannerFragment.newInstance()) }
    }

    companion object {
        @JvmStatic
        fun newInstance(index: Int) =
            SafeAccountSettingsFragment().apply {
                arguments = Bundle().apply {
                    putInt(INDEX, index)
                }
            }
    }
}
