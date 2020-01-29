package minerva.android.values.transaction.fragment

import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        prepareObservers()
        calculateTransactionCost()
        prepareTextListeners()
    }

    override fun onPause() {
        super.onPause()
        validationDisposable?.dispose()
    }

    private fun prepareObservers() {
        viewModel.apply {
            sendTransactionLiveData.observe(this@TransactionsFragment, EventObserver { listener.onResult(true, it) })
            errorTransactionLiveData.observe(this@TransactionsFragment, EventObserver { listener.onResult(false, it) })
            transactionCostLiveData.observe(this@TransactionsFragment, EventObserver { setTransactionsCosts(it) })
            errorLiveData.observe(this@TransactionsFragment, Observer { showErrorFlashBar() })
            loadingLiveData.observe(this@TransactionsFragment, EventObserver { if (it) showLoader() else hideLoader() })
        }
    }

    //TODO subscribe should be in ViewModel and connected with Fragment by LiveData
    private fun prepareTextListeners() {
        validationDisposable = Observable.combineLatest(
            amount.getValidationObservable(amountInputLayout) { Validator.validateIsFilled(it) },
            receiver.getValidationObservable(receiverInputLayout) { Validator.validateIsFilled(it) },
            gasLimit.getValidationObservable(gasLimitInputLayout) { Validator.validateIsFilled(it) },
            gasPrice.getValidationObservable(gasPriceInputLayout) { Validator.validateIsFilled(it) },
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
            getString(R.string.transactions_cost_error_title),
            getString(R.string.transactions_cost_error_message)
        )
    }

    private fun setTransactionsCosts(costs: Triple<BigDecimal, BigInteger, BigDecimal>) {
        gasPrice.setText(costs.first.toPlainString())
        gasLimit.setText(costs.second.toString())
        transactionCost.text = getString(R.string.transaction_cost, costs.third.toPlainString(), viewModel.network)
    }

    private fun calculateTransactionCost() {
        setGasPriceOnTextChangedListener()
        setGasLimitOnTextChangedListener()
    }

    private fun setGasLimitOnTextChangedListener() {
        gasLimit.afterTextChanged { gasLimit ->
            if (canCalculateTransactionCost(gasLimit, gasPrice)) {
                setTransactionCost(getGasPrice(), gasLimit.toBigInteger())
            } else {
                clearTransactionCost()
            }
        }
    }

    private fun canCalculateTransactionCost(input: String, editText: EditText) =
        input.isNotEmpty() && editText.text?.isNotEmpty() == true

    private fun setGasPriceOnTextChangedListener() {
        gasPrice.afterTextChanged { gasPrice ->
            if (canCalculateTransactionCost(gasPrice, gasLimit)) {
                setTransactionCost(gasPrice.toBigDecimal(), getGasLimit())
            } else {
                clearTransactionCost()
            }
        }
    }

    private fun clearTransactionCost() {
        transactionCost.text = getString(R.string.transaction_cost, ZER0, viewModel.network)
    }

    private fun setTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger) {
        transactionCost.text = getString(
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
//        TODO include transaction cost, balance should be lower
        amount.onRightDrawableClicked {
            it.setText(viewModel.value.balance.toString())
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
            viewModel.sendTransaction(receiver.text.toString(), amount.text.toString(), getGasPrice(), getGasLimit())
        }
    }

    private fun getGasLimit() = BigInteger(gasLimit.text.toString())

    private fun getGasPrice() = BigDecimal(gasPrice.text.toString())

    private fun setupTexts() {
        viewModel.network.apply {
            amountInputLayout.hint = "${getString(R.string.amount)}($this)"
            sendButton.text = "${getString(R.string.send)} $this"
        }
    }

    fun setReceiver(result: String?) {
        result?.let {
            receiver.setText(result)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionsFragment()

        private val ZER0 = "0"
    }
}
