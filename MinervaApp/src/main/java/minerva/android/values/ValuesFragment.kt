package minerva.android.values

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.values.adapter.ValueAdapter
import minerva.android.values.listener.ValuesFragmentToAdapterListener
import minerva.android.walletmanager.model.Value
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class ValuesFragment : Fragment(), ValuesFragmentToAdapterListener {

    private val viewModel: ValuesViewModel by viewModel()
    private val valueAdapter = ValueAdapter(this)
    private lateinit var listener: FragmentInteractorListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView(view)
        setupLiveData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.apply {
            onResume()
            refreshBalances()
            getAssetBalance()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as FragmentInteractorListener
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onSendTransaction(value: Value) = listener.showSendTransactionScreen(value)

    override fun onSendAssetTransaction(valueIndex: Int, assetIndex: Int) {
        listener.showSendAssetTransactionScreen(valueIndex, assetIndex)
    }

    override fun onCreateSafeAccount(value: Value) = viewModel.createSafeAccount(value)

    override fun onValueRemove(value: Value) = showRemoveDialog(value)

    private fun setupRecycleView(view: View) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = valueAdapter
        }
    }

    private fun setupLiveData() {
        viewModel.apply {
            walletConfigLiveData.observe(this@ValuesFragment, Observer {
                noDataMessage.visibleOrGone(it.hasActiveValue)
                valueAdapter.updateList(it.values)
            })
            balanceLiveData.observe(this@ValuesFragment, Observer { valueAdapter.updateBalances(it) })
            assetBalanceLiveData.observe(this@ValuesFragment, Observer { valueAdapter.updateAssetBalances(it) })
            errorLiveData.observe(this@ValuesFragment, Observer {
                showErrorFlashbar(getString(R.string.error_header), it.peekContent().message)
            })
            noFundsLiveData.observe(this@ValuesFragment, Observer {
                MinervaFlashbar.show(requireActivity(), getString(R.string.no_funds), getString(R.string.no_funds_message))
            })
            loadingLiveData.observe(this@ValuesFragment, EventObserver {
                listener.shouldShowLoadingScreen(it)
            })
            balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.observe(this@ValuesFragment, EventObserver {
                showErrorFlashbar(getString(R.string.cannot_remove_safe_account_title), getString(R.string.cannot_remove_safe_account_message))
            })
            isNotSafeAccountMasterOwnerErrorLiveData.observe(this@ValuesFragment, EventObserver {
                showErrorFlashbar(getString(R.string.error_header), getString(R.string.safe_account_removal_error))
            })
        }
    }

    private fun showErrorFlashbar(title: String, message: String? = String.Empty) =
        message?.let {
            MinervaFlashbar.show(requireActivity(), title, it)
        }.orElse {
            MinervaFlashbar.show(requireActivity(), title, getString(R.string.unexpected_error))
        }

    private fun showRemoveDialog(value: Value) {
        context?.let { context ->
            MaterialAlertDialogBuilder(context, R.style.AlertDialogMaterialTheme)
                .setBackground(context.getDrawable(R.drawable.rounded_white_background))
                .setTitle(value.name)
                .setMessage(R.string.remove_value_dialog_message)
                .setPositiveButton(R.string.remove) { dialog, _ ->
                    viewModel.removeValue(value)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
