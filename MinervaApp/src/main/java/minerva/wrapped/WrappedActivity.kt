package minerva.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.identities.EditIdentityFragment
import minerva.android.kotlinUtils.InvalidIndex
import java.util.*

class WrappedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrapped)

        val fragmentType = getFragmentType()
        prepareActionBar(fragmentType)
        prepareFragment(fragmentType)
    }

    private fun prepareFragment(fragmentType: WrappedFragmentType) {
        val fragment = when(fragmentType) {
            WrappedFragmentType.IDENTITY -> EditIdentityFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            //else fragments
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun getDefaultTitle(fragmentType: WrappedFragmentType) =
        when (fragmentType) {
            WrappedFragmentType.IDENTITY -> getString(R.string.new_identity)
        }

    private fun prepareActionBar(fragmentType: WrappedFragmentType) {
        supportActionBar?.apply {
            title = intent.getStringExtra(TITLE) ?: getDefaultTitle(fragmentType)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun getFragmentType() = intent.getSerializableExtra(FRAGMENT) as WrappedFragmentType?
            ?: throw MissingFormatArgumentException("No fragment was passed to Activity")

    companion object {
        const val TITLE = "title"
        const val INDEX = "index"
        const val FRAGMENT = "fragment"
    }
}

enum class WrappedFragmentType {
    IDENTITY
}
