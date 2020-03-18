package minerva.android.values.akm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_safe_account_settings.*
import minerva.android.R
import minerva.android.extension.getValidationObservable
import minerva.android.extension.onRightDrawableClicked
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.values.adapter.OwnerAdapter
import minerva.android.values.akm.AddressScannerFragment.Companion.SCANNER_FRAGMENT
import minerva.android.values.listener.OnOwnerRemovedListener
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import minerva.android.wrapped.WrappedActivityListener
import org.koin.androidx.viewmodel.ext.android.viewModel

class SafeAccountSettingsFragment : Fragment(), OnOwnerRemovedListener {

    private val viewModel: SafeAccountSettingsViewModel by viewModel()
    private val ownerAdapter = OwnerAdapter(this)
    private val safeAccountDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var listener: WrappedActivityListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as WrappedActivityListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_safe_account_settings, container, false)

    override fun onOwnerRemoved(index: Int) = viewModel.removeOwner(index)

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
        viewModel.onPause()
        safeAccountDisposable.dispose()
    }

    private fun initializeFragment() {
        setupRecycleView()
        prepareAddButton()
        setAddressScannerListener()
        activity?.window?.setSoftInputMode(SOFT_INPUT_ADJUST_PAN)
        listener.extraStringLiveData.observe(this, EventObserver { newOwner.setText(it) })
        viewModel.apply {
            ownersLiveData.observe(this@SafeAccountSettingsFragment, Observer { ownerAdapter.updateList(it) })
            errorLiveData.observe(this@SafeAccountSettingsFragment, EventObserver {
                MinervaFlashbar.show(requireActivity(), getString(R.string.error_header), getString(R.string.unexpected_error))
            })
            arguments?.let { loadValue(it.getInt(INDEX)) }
        }

    }

    private fun prepareAddButton() {
        //TODO need to be refactored
        addOwnerButton.setOnClickListener {
            viewModel.addOwner(newOwner.text.toString())
            newOwner.clearFocus()
            //hacks
            safeAccountDisposable.clear()
            addOwnerButton.isEnabled = false
            newOwner.text?.clear()
            prepareTextValidator()
            //end of hack
        }
    }

    private fun prepareTextValidator() {
        safeAccountDisposable.add(newOwner.getValidationObservable(ownerAddressInputLayout) { Validator.validateReceiverAddress(it) }
            .subscribeBy(
                onNext = { addOwnerButton.isEnabled = it },
                onError = { addOwnerButton.isEnabled = false }
            )
        )
    }

    private fun setupRecycleView() {
        owners.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ownerAdapter
        }
    }

    private fun setAddressScannerListener() {
        newOwner.onRightDrawableClicked { listener.showScanner()}
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
