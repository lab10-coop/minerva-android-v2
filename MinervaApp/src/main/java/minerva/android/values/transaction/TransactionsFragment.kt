package minerva.android.values.transaction

import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_transactions.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.kotlinUtils.Empty
import minerva.android.values.listener.TransactionFragmentsListener

class TransactionsFragment : Fragment() {

    private lateinit var network: String
    private var areTransactionCostsOpen = false
    private lateinit var listener: TransactionFragmentsListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTexts()
        setupListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            network = it.getString(NETWORK_NAME, String.Empty)
        }
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
//        todo add getting all balance
        amount.onRightDrawableClicked {
            Toast.makeText(context, "ALL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setGetAllBalanceListener() {
//        todo add showing scanner
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
//        todo handle transaction result
        sendButton.setOnClickListener {
            listener.onResult(true)
        }
    }

    private fun setupTexts() {
        amountInputLayout.hint = "${getString(R.string.amount)}($network)"
        sendButton.text = "${getString(R.string.send)} $network"
//        todo add transaction cost
        transactionCost.text = "${transactionCost.text} ~0.01 $network"
    }

    companion object {
        @JvmStatic
        fun newInstance(network: String) = TransactionsFragment().apply {
            arguments = Bundle().apply {
                putString(NETWORK_NAME, network)
            }
        }

        private const val NETWORK_NAME = "value"
    }
}
