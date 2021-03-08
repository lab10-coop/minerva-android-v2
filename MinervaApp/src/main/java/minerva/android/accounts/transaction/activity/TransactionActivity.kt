package minerva.android.accounts.transaction.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import minerva.android.R
import minerva.android.accounts.address.AddressFragment
import minerva.android.accounts.listener.TransactionListener
import minerva.android.accounts.transaction.fragment.TransactionSendFragment
import minerva.android.accounts.transaction.fragment.TransactionViewModel
import minerva.android.accounts.transaction.fragment.adapter.TransactionPagerAdapter
import minerva.android.databinding.ActivityTransactionBinding
import minerva.android.extension.addFragmentWithBackStack
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.onTabSelected
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.WrappedFragmentType
import org.koin.androidx.viewmodel.ext.android.viewModel

class TransactionActivity : AppCompatActivity(), TransactionListener {

    private val viewModel: TransactionViewModel by viewModel()
    private lateinit var binding: ActivityTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.apply {
            accountIndex = intent.getIntExtra(ACCOUNT_INDEX, Int.InvalidIndex)
            getAccount(accountIndex, intent.getIntExtra(ASSET_INDEX, Int.InvalidIndex))
        }
        initView(intent.getIntExtra(TRANSACTION_SCREEN, SEND_TRANSACTION_INDEX))
    }

    private fun initView(initPageIndex: Int) {
        prepareActionBar()
        setupViewPager(initPageIndex)
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    private fun setupViewPager(initPageIndex: Int) {
        with(binding) {
            transactionViewPager.apply {
                adapter = TransactionPagerAdapter(this@TransactionActivity, ::getFragment)
                setCurrentItem(SEND_TRANSACTION_INDEX, false)

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        transactionTab.let {
                            it.selectTab(it.getTabAt(position))
                        }
                    }
                })
            }

            transactionTab.apply {
                addTab(newTab().setText(getString(R.string.send)))
                addTab(newTab().setText(getString(R.string.receive)))
            }

            transactionTab.onTabSelected {
                invalidateOptionsMenu()
                transactionViewPager.setCurrentItem(it, true)
            }
            transactionViewPager.setCurrentItem(initPageIndex, false)
        }
    }

    private fun getFragment(position: Int) =
        when (position) {
            SEND_TRANSACTION_INDEX -> TransactionSendFragment.newInstance()
            else -> AddressFragment.newInstance(WrappedFragmentType.ACCOUNT_ADDRESS, viewModel.accountIndex)
        }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            show()
            title = " ${viewModel.account.name}"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onTransactionAccepted(message: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(ACCOUNT_INDEX, getAccountIndex())
            putExtra(IS_TRANSACTION_SUCCESS, true)
            putExtra(TRANSACTION_MESSAGE, message)
        })
        finish()
    }

    private fun getAccountIndex() =
        /*Subscription to web sockets doesn't work with http rpc, hence when there is no wss uri, index of account is not taken into consideration*/
        with(viewModel) {
            if (wssUri == String.Empty || isTokenTransaction || isSafeAccountTokenTransaction) Int.InvalidIndex
            else account.id
        }

    override fun onError(message: String) {
        MinervaFlashbar.show(
            this,
            getString(R.string.transaction_error_title),
            getString(R.string.transaction_error_message, message)
        )
    }

    override fun showScanner(fragment: BaseScannerFragment) {
        supportActionBar?.hide()
        addFragmentWithBackStack(
            R.id.container,
            fragment,
            R.animator.slide_in_left,
            R.animator.slide_out_right
        )
    }

    override fun setScanResult(text: String?) {
        onBackPressed()
        (getCurrentFragment() as? TransactionSendFragment)?.setReceiver(text)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    companion object {
        const val SEND_TRANSACTION_INDEX = 0
        const val IS_TRANSACTION_SUCCESS = "is_transaction_succeed"
        const val TRANSACTION_MESSAGE = "transaction_message"
        const val ASSET_INDEX = "asset_index"
        const val TRANSACTION_SCREEN = "transaction_screen"
    }
}
