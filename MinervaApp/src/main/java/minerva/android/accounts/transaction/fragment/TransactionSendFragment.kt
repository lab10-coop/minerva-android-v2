package minerva.android.accounts.transaction.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.R
import minerva.android.accounts.listener.TransactionListener
import minerva.android.accounts.transaction.fragment.TransactionViewModel.Companion.ONE_ELEMENT
import minerva.android.accounts.transaction.fragment.adapter.RecipientAdapter
import minerva.android.accounts.transaction.fragment.adapter.TokenAdapter
import minerva.android.accounts.transaction.fragment.scanner.TransactionAddressScanner
import minerva.android.databinding.FragmentTransactionSendBinding
import minerva.android.extension.*
import minerva.android.extension.validator.Validator
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.properties.Delegates

class TransactionSendFragment : Fragment(R.layout.fragment_transaction_send) {

    private var areTransactionCostsOpen = false
    private var shouldOverrideTransactionCost = true
    private lateinit var listener: TransactionListener
    private val viewModel: TransactionViewModel by sharedViewModel()
    private var validationDisposable: Disposable? = null
    private var allPressed: Boolean = false
    private lateinit var binding: FragmentTransactionSendBinding

    private var txCostObservable: BigDecimal by Delegates.observable(BigDecimal.ZERO) { _, oldValue: BigDecimal, newValue: BigDecimal ->
        binding.apply {
            transactionCostAmount.text =
                getString(R.string.transaction_cost_amount, newValue.toPlainString(), viewModel.token)
            if (allPressed && oldValue != newValue) {
                if (viewModel.recalculateAmount <= BigDecimal.ZERO) amount.setText(BigDecimal.ZERO.toPlainString())
                else amount.setText(viewModel.recalculateAmount.toPlainString())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransactionSendBinding.bind(view)
        prepareTokenDropdown()
        setupTexts()
        setupListeners()
        binding.transactionCostAmount.text = getString(R.string.transaction_cost_amount, EMPTY_VALUE, viewModel.token)
        prepareRecipients()
        prepareObservers()
    }

    override fun onResume() {
        super.onResume()
        prepareTextListeners()
    }

    override fun onPause() {
        super.onPause()
        validationDisposable?.dispose()
    }

    fun setReceiver(result: String?) {
        result?.let {
            binding.receiver.setText(result)
        }
    }

    private fun prepareObservers() {
        viewModel.apply {
            transactionCompletedLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onTransactionAccepted(getString(R.string.refresh_balance_to_check_transaction_status)) })
            sendTransactionLiveData.observe(viewLifecycleOwner, EventObserver { handleTransactionStatus(it) })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                it.message?.let { message -> showErrorFlashBar(message) } ?: showErrorFlashBar()
            })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { if (it) showLoader() else hideLoader() })
            saveWalletActionFailedLiveData.observe(viewLifecycleOwner, EventObserver { listener.onError(it.first) })
            transactionCostLiveData.observe(viewLifecycleOwner, EventObserver { handleTransactionCosts(it) })
            transactionCostLoadingLiveData.observe(viewLifecycleOwner, EventObserver { handleTransactionCostLoader(it) })
            overrideTxCostLiveData.observe(viewLifecycleOwner, EventObserver { shouldOverrideTransactionCost = true })
        }
    }

    private fun handleTransactionCosts(it: TransactionCost) {
        with(binding) {
            transactionCostLayout.isEnabled = true
            arrow.visible()
        }
        handleGasLimitDefaultValue(it)
        if (shouldOverrideTransactionCost) {
            txCostObservable = it.cost
            setTransactionsCosts(it)
        }
    }

    private fun handleGasLimitDefaultValue(it: TransactionCost) {
        if (it.isGasLimitDefaultValue) {
            Toast.makeText(context, getString(R.string.estimate_transaction_cost_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun handleTransactionCostLoader(showLoader: Boolean) {
        binding.apply {
            transactionCostProgressBar.visibleOrGone(showLoader)
            transactionCostAmount.visibleOrGone(!showLoader)
        }
    }

    private fun handleTransactionStatus(status: Pair<String, Int>) {
        when (status.second) {
            WalletActionStatus.SENT -> listener.onTransactionAccepted(status.first)
            WalletActionStatus.FAILED -> listener.onError(status.first)
        }
    }

    private fun prepareTextListeners() {
        with(binding) {
            validationDisposable = Observable.combineLatest(
                amount.getValidationObservable(amountInputLayout) {
                    Validator.validateAmountField(it, viewModel.cryptoBalance)
                },
                receiver.getValidationObservable(receiverInputLayout) {
                    Validator.validateAddress(it, viewModel.isAddressValid(it), R.string.invalid_account_address)
                },
                BiFunction<Boolean, Boolean, Boolean> { isAmountValid, isAddressValid -> isAmountValid && isAddressValid })
                .map { areFieldsValid ->
                    if (areFieldsValid) {
                        viewModel.getTransactionCosts(receiver.text.toString(), getAmount())
                        transactionCostLayout.hideKeyboard()
                        receiver.dismissDropDown()
                    } else {
                        arrow.gone()
                        clearTransactionCost()
                        transactionCostLayout.isEnabled = false
                        if (areTransactionCostsOpen) {
                            closeTransactionCost()
                        }
                    }
                    areFieldsValid
                }.switchMap {
                    Observable.combineLatest(
                        gasLimitEditText.getValidationObservable(gasLimitInputLayout) { Validator.validateIsFilled(it) },
                        gasPriceEditText.getValidationObservable(gasPriceInputLayout) { Validator.validateIsFilled(it) },
                        BiFunction<Boolean, Boolean, Boolean> { isGasLimitValid, isGasPriceValid ->
                            isGasLimitValid && isGasPriceValid && it
                        }
                    )
                }
                .subscribeBy(
                    onNext = {
                        viewModel.isTransactionAvailable(it).let { isAvailable ->
                            sendButton.isEnabled = isAvailable
                            balanceToLowError.visibleOrGone(!isAvailable)
                            transactionCostAmount.setTextColor(getTransactionCostColor(isAvailable))
                        }
                    },
                    onError = {
                        sendButton.isEnabled = false
                        balanceToLowError.visibleOrGone(false)
                    }
                )
        }
    }

    private fun getTransactionCostColor(isAvailable: Boolean) =
        (if (isAvailable) R.color.gray
        else R.color.errorRed).let {
            ContextCompat.getColor(requireContext(), it)
        }

    private fun prepareTokenDropdown() {
        binding.apply {
            tokenSpinner.apply {
                viewModel.tokensList.let { tokens ->
                    setBackgroundResource(getSpinnerBackground(tokens.size))
                    isEnabled = isSpinnerEnabled(tokens.size)
                    adapter = TokenAdapter(context, R.layout.spinner_token, tokens)
                        .apply { setDropDownViewResource(R.layout.spinner_token) }
                    setSelection(viewModel.spinnerPosition, false)
                    setPopupBackgroundResource(R.drawable.rounded_white_background)
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            viewModel.updateTokenAddress(position - ONE_ELEMENT)
                            setupTexts()
                        }

                        override fun onNothingSelected(adapterView: AdapterView<*>?) =
                            setSelection(viewModel.spinnerPosition, true)
                    }
                }
            }
        }
    }

    private fun prepareRecipients() {
        viewModel.loadRecipients()
        binding.receiver.apply {
            onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                (parent.getItemAtPosition(position) as Recipient).let { recipient -> setText(recipient.getData()) }
            }
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
            setAdapter(RecipientAdapter(context, R.layout.recipient_list_row, viewModel.recipients))
            setDropDownBackgroundResource(R.drawable.drop_down_menu_background)
            dropDownVerticalOffset = DROP_DOWN_VERTICAL_OFFSET
            threshold = MIN_SIGN_TO_FILTER
        }
    }

    private fun hideLoader() {
        binding.apply {
            sendButton.visible()
            sendTransactionProgressBar.invisible()
        }
    }

    private fun showLoader() {
        binding.apply {
            sendTransactionProgressBar.visible()
            sendButton.invisible()
        }
    }

    private fun showErrorFlashBar(message: String = getString(R.string.transactions_error_message)) {
        MinervaFlashbar.show(requireActivity(), getString(R.string.transactions_error_title), message)
    }

    private fun getSpinnerBackground(size: Int) =
        if (size > ONE_ELEMENT) R.drawable.rounded_spinner_background
        else R.drawable.rounded_white_background

    private fun isSpinnerEnabled(size: Int) = size > ONE_ELEMENT

    private fun setTransactionsCosts(transactionCost: TransactionCost) {
        with(binding) {
            transactionCostLayout.isEnabled = true
            transactionCost.let {
                gasPriceEditText.setText(it.gasPrice.toPlainString())
                gasLimitEditText.setText(it.gasLimit.toString())
            }
            setGasPriceOnTextChangedListener()
            setGasLimitOnTextChangedListener()
        }
    }

    private fun setGasLimitOnTextChangedListener() {
        binding.apply {
            gasLimitEditText.afterTextChanged { gasLimit ->
                if (canCalculateTransactionCost(gasLimit, gasPriceEditText)) {
                    shouldOverrideTransactionCost = false
                    calculateTransactionCost(getGasPrice(), gasLimit.toBigInteger())
                } else {
                    clearTransactionCost()
                }
            }
        }
    }

    private fun setGasPriceOnTextChangedListener() {
        binding.apply {
            gasPriceEditText.afterTextChanged { gasPrice ->
                if (canCalculateTransactionCost(gasPrice, gasLimitEditText)) {
                    shouldOverrideTransactionCost = false
                    calculateTransactionCost(gasPrice.toBigDecimal(), getGasLimit())
                } else {
                    clearTransactionCost()
                }
            }
        }
    }

    private fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger) {
        txCostObservable = viewModel.calculateTransactionCost(gasPrice, gasLimit)
    }

    private fun setAddressScannerListener() {
        binding.amount.onRightDrawableClicked {
            allPressed = true
            it.setText(viewModel.getAllAvailableFunds())
        }
    }

    private fun canCalculateTransactionCost(input: String, editText: EditText) =
        input.isNotEmpty() && editText.text?.isNotEmpty() == true

    private fun clearTransactionCost() {
        binding.transactionCostAmount.text = getString(R.string.transaction_cost_amount, EMPTY_VALUE, viewModel.token)
        viewModel.transactionCost = BigDecimal.ZERO
        allPressed = false
        shouldOverrideTransactionCost = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as TransactionListener
    }

    private fun setupListeners() {
        binding.transactionCostLayout.isEnabled = false
        setSendButtonOnClickListener()
        setOnTransactionCostOnClickListener()
        setGetAllBalanceListener()
        setAddressScannerListener()
    }

    private fun setGetAllBalanceListener() {
        binding.receiver.onRightDrawableClicked {
            listener.showScanner(TransactionAddressScanner.newInstance(viewModel.accountIndex))
        }
    }

    private fun setOnTransactionCostOnClickListener() {
        binding.apply {
            transactionCostLayout.setOnClickListener {
                TransitionManager.beginDelayedTransition(transactionView)
                if (areTransactionCostsOpen) closeTransactionCost() else openTransactionCost()
            }
        }
    }

    private fun openTransactionCost() {
        areTransactionCostsOpen = true
        binding.apply {
            transactionCostLayout.apply {
                arrow.rotate180()
                gasPriceInputLayout.visible()
                gasLimitInputLayout.visible()
            }
        }
    }

    private fun closeTransactionCost() {
        areTransactionCostsOpen = false
        binding.apply {
            transactionCostLayout.apply {
                arrow.rotate180back()
                gasPriceInputLayout.gone()
                gasLimitInputLayout.gone()
            }
        }
    }

    private fun setSendButtonOnClickListener() {
        binding.apply {
            sendButton.setOnClickListener {
                viewModel.run {
                    if (isAuthenticationEnabled()) showBiometricPrompt {
                        sendTransaction(
                            receiver.text.toString(),
                            getAmount(),
                            getGasPrice(),
                            getGasLimit()
                        )
                    }
                    else sendTransaction(receiver.text.toString(), getAmount(), getGasPrice(), getGasLimit())
                }
            }
            receiverInputLayout.setEndIconOnClickListener {
                receiver.setText(String.Empty)
                sendButton.isEnabled = false
            }
        }
    }

    private fun getAmount() = BigDecimal(binding.amount.text.toString())

    private fun getGasLimit() = BigInteger(binding.gasLimitEditText.text.toString())

    private fun getGasPrice() = BigDecimal(binding.gasPriceEditText.text.toString())

    @SuppressLint("SetTextI18n")
    private fun setupTexts() {
        binding.apply {
            viewModel.prepareCurrency().let {
                amountInputLayout.hint = "${getString(R.string.amount)} ($it)"
                sendButton.text = "${getString(R.string.send)} $it"
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionSendFragment()

        private const val EMPTY_VALUE = "-.--"
        private const val DROP_DOWN_VERTICAL_OFFSET = 8
        private const val MIN_SIGN_TO_FILTER = 3
    }
}
