package minerva.android.values.transaction.fragment

import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function4
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_transactions.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.values.listener.TransactionFragmentsListener
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.values.transaction.fragment.adapter.RecipientAdapter
import minerva.android.walletmanager.model.Recipient
import minerva.android.walletmanager.model.TransactionCost
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.math.BigDecimal
import java.math.BigInteger

class TransactionsFragment : Fragment() {

    private var areTransactionCostsOpen = false
    private lateinit var listener: TransactionFragmentsListener
    private val viewModel: TransactionsViewModel by sharedViewModel()
    private var validationDisposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTexts()
        setupListeners()
        viewModel.getTransactionCosts()
        prepareRecipients()
        prepareObservers()
        calculateTransactionCost()
        prepareTextListeners()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
        validationDisposable?.dispose()
    }

    fun setReceiver(result: String?) {
        result?.let {
            receiver.setText(result)
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            sendTransactionLiveData.observe(this@TransactionsFragment, EventObserver { handleTransactionStatus(it) })
            transactionCostLiveData.observe(this@TransactionsFragment, EventObserver { setTransactionsCosts(it) })
            errorLiveData.observe(this@TransactionsFragment, Observer { showErrorFlashBar() })
            loadingLiveData.observe(this@TransactionsFragment, EventObserver { if (it) showLoader() else hideLoader() })
            saveWalletActionFailedLiveData.observe(this@TransactionsFragment, EventObserver {
                listener.onResult(true, it.first)
            })
        }
    }

    private fun handleTransactionStatus(status: Pair<String, Int>) {
        if (status.second == WalletActionStatus.SENT) {
            listener.onResult(true, status.first)
        } else if (status.second == WalletActionStatus.FAILED) {
            listener.onResult(false, status.first)
        }
    }

    private fun prepareTextListeners() {
        validationDisposable = Observable.combineLatest(
            amount.getValidationObservable(amountInputLayout) { Validator.validateAmountField(it, viewModel.getBalance()) },
            receiver.getValidationObservable(receiverInputLayout) { Validator.validateReceiverAddress(it) },
            gasLimitEditText.getValidationObservable(gasLimitInputLayout) { Validator.validateIsFilled(it) },
            gasPriceEditText.getValidationObservable(gasPriceInputLayout) { Validator.validateIsFilled(it) },
            Function4<Boolean, Boolean, Boolean, Boolean, Boolean> { isAmountValid, isReceiverValid, isGasLimitValid, isGasPriceValid ->
                isAmountValid && isReceiverValid && isGasLimitValid && isGasPriceValid
            }
        ).subscribeBy(
            onNext = {
                sendButton.isEnabled = it
            },
            onError = {
                sendButton.isEnabled = false
            }
        )
    }

    private fun prepareRecipients() {
        viewModel.loadRecipients()
        receiver.apply {
            onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                (parent.getItemAtPosition(position) as Recipient).let { recipient -> receiver.setText(recipient.getData()) }
            }
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) receiver.showDropDown() }
            setAdapter(RecipientAdapter(context, R.layout.recipient_list_row, viewModel.recipients))
            setDropDownBackgroundResource(R.drawable.drop_down_menu_background)
            dropDownVerticalOffset = DROP_DOWN_VERTICAL_OFFSET
            threshold = MIN_SIGN_TO_FILTER
        }
    }

    private fun hideLoader() {
        sendButton.visible()
        sendTransactionProgressBar.invisible()
    }

    private fun showLoader() {
        sendTransactionProgressBar.visible()
        sendButton.invisible()
    }

    private fun showErrorFlashBar() {
        MinervaFlashbar.show(
            requireActivity(),
            getString(R.string.transactions_error_title),
            getString(R.string.transactions_error_message)
        )
    }

    private fun setTransactionsCosts(transactionCost: TransactionCost) {
        transactionCost.apply {
            gasPriceEditText.setText(gasPrice.toPlainString())
            gasLimitEditText.setText(gasLimit.toString())
            transactionCostLabel.text = getString(R.string.transaction_cost, cost.toPlainString(), viewModel.network)
        }
    }

    private fun calculateTransactionCost() {
        setGasPriceOnTextChangedListener()
        setGasLimitOnTextChangedListener()
    }

    private fun setGasLimitOnTextChangedListener() {
        gasLimitEditText.afterTextChanged { gasLimit ->
            if (canCalculateTransactionCost(gasLimit, gasPriceEditText)) {
                setTransactionCost(getGasPrice(), gasLimit.toBigInteger())
            } else {
                clearTransactionCost()
            }
        }
    }

    private fun canCalculateTransactionCost(input: String, editText: EditText) =
        input.isNotEmpty() && editText.text?.isNotEmpty() == true

    private fun setGasPriceOnTextChangedListener() {
        gasPriceEditText.afterTextChanged { gasPrice ->
            if (canCalculateTransactionCost(gasPrice, gasLimitEditText)) {
                setTransactionCost(gasPrice.toBigDecimal(), getGasLimit())
            } else {
                clearTransactionCost()
            }
        }
    }

    private fun clearTransactionCost() {
        transactionCostLabel.text = getString(R.string.transaction_cost, ZER0, viewModel.network)
    }

    private fun setTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger) {
        transactionCostLabel.text = getString(
            R.string.transaction_cost,
            viewModel.calculateTransactionCost(gasPrice, gasLimit),
            viewModel.network
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as TransactionFragmentsListener
    }

    private fun setupListeners() {
        setSendButtonOnClickListener()
        setOnTransactionCostOnClickListener()
        setGetAllBalanceListener()
        setAddressScannerListener()
    }

    private fun setAddressScannerListener() {
        amount.onRightDrawableClicked {
            it.setText(viewModel.getAllAvailableFunds())
        }
    }

    private fun setGetAllBalanceListener() {
        receiver.onRightDrawableClicked {
            listener.showScanner()
        }
    }

    private fun setOnTransactionCostOnClickListener() {
        transactionCostLayout.setOnClickListener {
            TransitionManager.beginDelayedTransition(transactionView)
            if (areTransactionCostsOpen) closeTransactionCost() else openTransactionCost()
        }
    }

    private fun openTransactionCost() {
        areTransactionCostsOpen = true
        transactionCostLayout.apply {
            arrow.rotate180()
            gasPriceInputLayout.visible()
            gasLimitInputLayout.visible()
        }
    }

    private fun closeTransactionCost() {
        areTransactionCostsOpen = false
        transactionCostLayout.apply {
            arrow.rotate180back()
            gasPriceInputLayout.gone()
            gasLimitInputLayout.gone()
        }
    }

    private fun setSendButtonOnClickListener() {
        sendButton.setOnClickListener {
            viewModel.sendTransaction(receiver.text.toString(), getAmount(), getGasPrice(), getGasLimit())
        }
    }

    private fun getAmount() = BigDecimal(amount.text.toString())

    private fun getGasLimit() = BigInteger(gasLimitEditText.text.toString())

    private fun getGasPrice() = BigDecimal(gasPriceEditText.text.toString())

    private fun setupTexts() {
        viewModel.prepareCurrency().apply {
            amountInputLayout.hint = "${getString(R.string.amount)}($this)"
            sendButton.text = "${getString(R.string.send)} $this"
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionsFragment()

        private const val ZER0 = "0"
        private const val DROP_DOWN_VERTICAL_OFFSET = 8
        private const val MIN_SIGN_TO_FILTER = 3
    }
}
