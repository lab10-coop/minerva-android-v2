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
import minerva.android.accounts.transaction.fragment.scanner.AddressScannerFragment
import minerva.android.databinding.ActivityTransactionBinding
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.onTabSelected
import minerva.android.extension.replaceFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
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
        viewModel.getAccount(
            intent.getIntExtra(ACCOUNT_INDEX, Int.InvalidIndex),
            intent.getIntExtra(ASSET_INDEX, Int.InvalidIndex)
        )
        initView()
    }

    private fun initView() {
        prepareActionBar()
        setupViewPager()
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    private fun setupViewPager() {
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
        }
    }

    private fun getFragment(position: Int) =
        when (position) {
            SEND_TRANSACTION_INDEX -> TransactionSendFragment.newInstance()
            else -> AddressFragment.newInstance(WrappedFragmentType.ACCOUNT_ADDRESS, viewModel.account.id)
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
        if (viewModel.wssUri == String.Empty) {
            Int.InvalidIndex
        } else {
            viewModel.account.id
        }

    override fun onError(message: String) {
        MinervaFlashbar.show(
            this,
            getString(R.string.transaction_error_title),
            getString(R.string.transaction_error_message, message)
        )
    }

    override fun showScanner() {
        supportActionBar?.hide()
        replaceFragment(
            R.id.container,
            AddressScannerFragment.newInstance(),
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
        private const val SEND_TRANSACTION_INDEX = 0
        const val IS_TRANSACTION_SUCCESS = "is_transaction_succeed"
        const val TRANSACTION_MESSAGE = "transaction_message"
        const val ACCOUNT_INDEX = "account_index"
        const val ASSET_INDEX = "asset_index"
    }
}
