package minerva.android.accounts.nft.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.databinding.ActivityNftCollectionBinding
import minerva.android.extension.addFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.main.MainActivity

class NftCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNftCollectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNftCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prepareActionBar()
        prepareFragment()
    }

    private fun prepareFragment() {
        val accountId = intent.getIntExtra(MainActivity.ACCOUNT_INDEX, Int.InvalidId)
        val collectionAddress = intent.getStringExtra(MainActivity.TOKEN_ADDRESS) ?: String.Empty
        addFragment(R.id.container, NftCollectionFragment.newInstance(accountId, collectionAddress))
    }

    private fun prepareActionBar() {
        val collectionName = intent.getStringExtra(COLLECTION_NAME)
        supportActionBar?.apply {
            show()
            title = collectionName ?: String.Empty
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nft_collection_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
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
}