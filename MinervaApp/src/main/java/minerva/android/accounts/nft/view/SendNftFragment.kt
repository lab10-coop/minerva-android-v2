package minerva.android.accounts.nft.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.gas_price_selector.view.*
import minerva.android.R
import minerva.android.accounts.listener.TransactionListener
import minerva.android.accounts.nft.model.NftItem
import minerva.android.accounts.nft.viewmodel.NftCollectionViewModel
import minerva.android.accounts.transaction.fragment.scanner.TransactionAddressScanner
import minerva.android.databinding.FragmentSendNftBinding
import minerva.android.extension.*
import minerva.android.extension.hideKeyboard
import minerva.android.extension.validator.ValidationResult
import minerva.android.extension.validator.Validator
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.walletconnect.transaction.TransactionSpeedAdapter
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxSpeed
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.RecyclableViewMoreTextView
import minerva.android.widget.dialog.walletconnect.GasLimitAndPriceDialog
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class SendNftFragment : Fragment(R.layout.fragment_send_nft) {

    private val viewModel: NftCollectionViewModel by activityViewModels()

    private lateinit var nftItem: NftItem
    private lateinit var binding: FragmentSendNftBinding
    private lateinit var listener: TransactionListener
    private var validationDisposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSendNftBinding.bind(view)
        requireActivity().invalidateOptionsMenu()
        setupObserver()
        setupNftSendForm()
        clearTransactionCost()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as TransactionListener
    }

    override fun onResume() {
        super.onResume()
        prepareTextListeners()
    }

    override fun onPause() {
        super.onPause()
        validationDisposable?.dispose()
    }

    private fun setupObserver() = with(viewModel) {
        selectedItemLiveData.observe(viewLifecycleOwner, Observer { item ->
            nftItem = item
            setupNftDetails()
        })
        transactionCostLiveData.observe(viewLifecycleOwner, EventObserver { txCost ->
            setTxCost(txCost, viewModel.account)
            viewModel.isAccountBalanceEnough().let { isEnough ->
                with(binding.sendNftForm) {
                    errorView.visibleOrInvisible(!isEnough)
                    transactionCost.setTextColor(getTransactionCostColor(isEnough))
                }
            }
        })
        transactionSpeedListLiveData.observe(viewLifecycleOwner, EventObserver { txCost ->
            setupGasPriceSelector(txCost)
        })
        sendTransactionLoadingLiveData.observe(viewLifecycleOwner, EventObserver { isShown ->
            handleTransactionSendInProgress(isShown)
        })
        transactionCompletedLiveData.observe(viewLifecycleOwner, EventObserver {
            listener.onTransactionAccepted(getString(R.string.refresh_balance_to_check_transaction_status))
        })
        saveWalletActionFailedLiveData.observe(viewLifecycleOwner, EventObserver { (balance, _) ->
            listener.onError(balance)
        })
        sendTransactionLiveData.observe(viewLifecycleOwner, EventObserver {
            handleTransactionStatus(it)
        })
        transactionCostLoadingLiveData.observe(viewLifecycleOwner, EventObserver {
            handleTransactionCostLoading(it)
        })
        transactionCostLoadingErrorLiveData.observe(viewLifecycleOwner, EventObserver {
            clearTransactionCost()
            Timber.e(it, "Unable to load transaction cost")
            MinervaFlashbar.showError(
                requireActivity(),
                Throwable(getString(R.string.failed_to_fetch_transaction_costs_error))
            )
        })
    }

    private fun handleTransactionStatus(status: Pair<String, Int>) {
        val (message, state) = status
        when (state) {
            WalletActionStatus.SENT -> listener.onTransactionAccepted(message)
            WalletActionStatus.FAILED -> listener.onError(message)
        }
    }

    private fun handleTransactionCostLoading(isLoading: Boolean) = with(binding.sendNftForm) {
        gasProgressBar.visibleOrGone(isLoading)
        gasLayout.visibleOrInvisible(!isLoading)
    }

    private fun setupNftDetails() {
        binding.setup(nftItem) { nftItem ->
            setupDescription(nftItem)
        }
    }

    private fun setupNftSendForm() {
        setupListeners()
        setupCustomGasPrice()
        binding.sendNftForm.amountInputLayout.visibleOrGone(viewModel.selectedItem.isERC1155)
    }

    private fun setupCustomGasPrice() {
        binding.sendNftForm.apply {
            editTxTime.setOnClickListener {
                GasLimitAndPriceDialog(requireContext(), viewModel.transactionCost.gasPrice.toPlainString(), viewModel.transactionCost.gasLimit.toString()) { gasPriceEnteredString, gasLimitEnteredString ->
                    try {
                        BigDecimal(gasPriceEnteredString).let { gasPrice ->
                            BigInteger.valueOf(gasLimitEnteredString.toLong()).let { gasLimit ->
                                viewModel.setGasLimit(gasLimit)
                                viewModel.setGasPrice(gasPrice)
                                setupCustomGasPrice(true)
                            }
                        }
                    } catch (e: java.lang.NumberFormatException) {
                        // Do nothing - simply close dialog
                    }
                }.apply {
                    show()
                    focusOnAmountAndShowKeyboard()
                }
            }

            closeCustomTime.setOnClickListener {
                setupCustomGasPrice(false)
                gasPriceSelector.setDefaultPositionWithoutSmoothAnimation(getRecentTxType())
                restoreRecentGasLimitAndPrice()
            }
        }
    }

    private fun restoreRecentGasLimitAndPrice() {
        viewModel.restoreGasLimit()
        getRecentGasPrice()?.let { viewModel.setGasPrice(it) } ?: run { clearTransactionCost() }
    }

    private fun setupCustomGasPrice(isAvailable: Boolean) {
        with(binding.sendNftForm) {
            closeCustomTime.visibleOrInvisible(isAvailable)
            editTxTime.visibleOrInvisible(!isAvailable)
            speed.visibleOrInvisible(isAvailable)
            transactionTime.visibleOrInvisible(isAvailable)
            gasPriceSelector.visibleOrInvisible(!isAvailable)
        }
    }

    private fun getRecentTxType() = viewModel.recentSelectedTxSpeed?.type ?: getDefaultTxType()
    private fun getRecentGasPrice() = with(binding.sendNftForm.gasPriceSelector.txSpeedViewPager) {
        (adapter as TransactionSpeedAdapter?)?.getCurrentTxSpeed(currentItem)?.value
    }


    private fun setupGasPriceSelector(speeds: List<TxSpeed>) {
        binding.sendNftForm.apply {
            gasPriceSelector.setAdapter(speeds) {
                onTxSpeedSelected(it)
            }
            getDefaultTxType().let { txType ->
                gasPriceSelector.setDefaultPositionWithoutSmoothAnimation(txType)
                restoreRecentGasLimitAndPrice()
            }
            setupCustomGasPrice(false)
        }
    }

    private fun onTxSpeedSelected(speed: TxSpeed) {
        viewModel.setGasPrice(speed.value)
        viewModel.recentSelectedTxSpeed = speed
    }

    private fun setupDescription(nftItem: NftItem) = binding.nftDetails.description.apply {
        listener = RecyclableViewMoreTextView.Listener.Inactive
        setOnClickListener { if (!isExpanded) toggle() }
        bind(nftItem.description, false)
    }


    private fun getDefaultTxType() =
        if (isMaticNetwork(viewModel.account.chainId)) TxType.STANDARD else TxType.FAST

    private fun isMaticNetwork(chainId: Int?) = chainId == ChainId.MATIC

    private fun setTxCost(txCost: TransactionCost, account: Account?) = with(binding.sendNftForm) {
        transactionCost.text = getString(
            R.string.transaction_cost_format,
            txCost.formattedCryptoCost,
            account?.network?.token,
            txCost.fiatCost
        )
    }


    private fun prepareTextListeners() {
        with(binding.sendNftForm) {
            validationDisposable = Observable.combineLatest(
                receiver.getValidationObservable(receiverInputLayout) {
                    Validator.validateAddress(
                        it,
                        viewModel.isAddressValid(it),
                        R.string.invalid_account_address
                    )
                },
                if (viewModel.isAmountAvailable()) amount.getValidationObservable(amountInputLayout) {
                    Validator.validateAmountFieldWithDecimalCheck(
                        it,
                        viewModel.getAllAvailableFunds(),
                        viewModel.selectedItem.decimals.toInt()
                    )
                } else Observable.just(true),
                BiFunction<Boolean, Boolean, Boolean> { isReceiverValid, isAmountValid ->
                    if (isReceiverValid) {
                        receiverInputLayout.hideKeyboard()
                        receiver.dismissDropDown()
                        scrollToSendButton()
                    }
                    isReceiverValid && isAmountValid
                }).map { isFormValid ->
                if (isFormValid) viewModel.getTransactionCosts(
                    receiver.text.toString(),
                    getAmount()
                )
                isFormValid
            }.subscribeBy(
                onNext = { isFormFilled ->
                    viewModel.isTransactionAvailable(isFormFilled).let { isAvailable ->
                        sendButton.isEnabled = isAvailable
                        errorView.visibleOrInvisible(!isAvailable && isFormFilled)
                        transactionCost.setTextColor(getTransactionCostColor(isAvailable))
                    }
                },
                onError = {
                    sendButton.isEnabled = false
                    errorView.visibleOrInvisible(false)
                }
            )
        }
    }

    private fun Validator.validateAmountFieldWithDecimalCheck(amount: String, balance: BigDecimal, decimals: Int = 0): ValidationResult {
        val validateAmountFieldResult = validateAmountField(amount, balance)
        return when {
            validateAmountFieldResult.hasError -> validateAmountFieldResult
            decimals == 0 && amount.contains(DOT) -> ValidationResult.error(R.string.amount_must_not_contain_decimal_digits)
            decimals < amount.lastIndex - amount.indexOf(DOT) && amount.contains(DOT) -> ValidationResult.error(R.string.too_many_digits_after_decimal)
            else -> ValidationResult(true)
        }
    }

    private fun getTransactionCostColor(isAvailable: Boolean) =
        (if (isAvailable) R.color.gray
        else R.color.errorRed).let {
            ContextCompat.getColor(requireContext(), it)
        }

    fun setReceiver(result: String?) {
        if (!result.isNullOrBlank()) {
            binding.sendNftForm.receiver.apply {
                setText(result)
                scrollToSendButton()
            }
        }
    }

    private fun scrollToSendButton() =
        binding.scrollView.scrollTo(0, binding.sendNftForm.sendButton.bottom)


    private fun setSendButtonOnClickListener() {
        binding.sendNftForm.apply {
            sendButton.setOnClickListener {
                if (viewModel.isAuthenticationEnabled()) showBiometricPrompt(::sendTransaction)
                else sendTransaction()
            }
        }
    }

    private fun sendTransaction() = viewModel.sendTransaction(
        binding.sendNftForm.receiver.text.toString(),
        getAmount()
    )

    private fun setupListeners() {
        setGetAllBalanceListener()
        setAddressScannerListener()
        setSendButtonOnClickListener()
    }

    private fun setAddressScannerListener() {
        binding.sendNftForm.receiver.onRightDrawableClicked {
            listener.showScanner(TransactionAddressScanner.newInstance(viewModel.account.id))
        }
    }

    private fun clearTransactionCost() {
        binding.sendNftForm.apply {
            transactionCost.text =
                getString(R.string.transaction_cost_amount, EMPTY_VALUE, viewModel.token)
            errorView.visibleOrInvisible(false)
            transactionCost.setTextColor(getTransactionCostColor(true))
        }
    }

    private fun handleTransactionSendInProgress(isInProgress: Boolean){
        binding.sendNftForm.apply {
            sendButton.visibleOrGone(!isInProgress)
            sendProgressBar.visibleOrGone(isInProgress)
        }
    }

    private fun setGetAllBalanceListener() {
        binding.sendNftForm.amount.onRightDrawableClicked {
            it.setText(viewModel.getAllAvailableFunds().toPlainString())
        }
    }

    private fun getAmount(): BigDecimal = if (viewModel.isAmountAvailable()) {
        val amountString = binding.sendNftForm.amount.text.toString()
        try {
            BigDecimal(amountString)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    } else {
        BigDecimal.ONE
    }


    companion object {
        @JvmStatic
        fun newInstance() = SendNftFragment()
        private const val EMPTY_VALUE = "-.--"
        private const val INVALID_INDEX = -1
        private const val DOT = "."
    }
}