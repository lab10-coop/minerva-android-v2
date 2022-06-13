package minerva.android.accounts.nft.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.accounts.listener.TransactionListener
import minerva.android.accounts.nft.model.NftItem
import minerva.android.accounts.nft.viewmodel.NftCollectionViewModel
import minerva.android.databinding.ActivityNftCollectionBinding
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.replaceFragmentWithBackStack
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.main.MainActivity
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.widget.MinervaFlashbar
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.viewModel


class NftCollectionActivity : AppCompatActivity(), TransactionListener {

    private lateinit var binding: ActivityNftCollectionBinding
    private val viewModel: NftCollectionViewModel by viewModel {
        parametersOf(
            intent.getIntExtra(MainActivity.ACCOUNT_INDEX, Int.InvalidId),
            intent.getStringExtra(MainActivity.TOKEN_ADDRESS) ?: String.Empty,
            intent.getBooleanExtra(MainActivity.IS_GROUP, false)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNftCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prepareActionBar()
        showCollectionFragment()
        setupObserver()
        initList()
    }

    private fun initList() = viewModel.getNftForCollection()

    private fun setupObserver() = with(viewModel) {
        selectedItemLiveData.observe(this@NftCollectionActivity, Observer { item ->
            if (item != NftItem.Invalid) {
                showSendNftFragment()
            }
        })
    }

    private fun showCollectionFragment() {
        addFragment(R.id.container, NftCollectionFragment.newInstance())
    }

    private fun prepareActionBar() {
        supportActionBar?.apply {
            show()
            prepareTitle()
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nft_collection_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            findItem(R.id.gridView)?.isVisible = shouldCollectionIconsBeVisible()
            findItem(R.id.timelineView)?.isVisible = shouldCollectionIconsBeVisible()
        }
        prepareActionBar()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun ActionBar.prepareTitle() {
        title = when (getCurrentFragment()) {
            is SendNftFragment -> getString(R.string.send)
            else -> intent.getStringExtra(COLLECTION_NAME) ?: String.Empty
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val COLLECTION_NAME = "collection_name"
    }

    override fun showScanner(fragment: BaseScannerFragment) {
        supportActionBar?.hide()
        replaceFragmentWithBackStack(
            R.id.container,
            fragment,
            R.animator.slide_in_left,
            R.animator.slide_out_right
        )
    }

    override fun setScanResult(text: String?) {
        onBackPressed()
        (getCurrentFragment() as? SendNftFragment)?.setReceiver(text)
    }

    override fun onBackPressed() {
        prepareActionBar()
        super.onBackPressed()
    }

    override fun onTransactionAccepted(message: String?) {
        MinervaFlashbar.show(this, getString(R.string.transaction_accepted_title), message ?: String.Empty)
        if (isCurrentFragmentSendNftFragment()) {
            onBackPressed()
        }
    }

    override fun onError(message: String) {
        MinervaFlashbar.show(
            this,
            getString(R.string.transaction_error_title),
            getString(R.string.transaction_error_message, message)
        )
    }


    private fun isCurrentFragmentNftCollectionFragment() =
        getCurrentFragment() is NftCollectionFragment

    private fun isCurrentFragmentSendNftFragment() = getCurrentFragment() is SendNftFragment

    private fun shouldCollectionIconsBeVisible() = isCurrentFragmentNftCollectionFragment()

    private fun showSendNftFragment() =
        replaceFragmentWithBackStack(
            R.id.container,
            SendNftFragment.newInstance(),
            R.animator.slide_in_left,
            R.animator.slide_out_right
        )
}