package minerva.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.identities.edit.EditIdentityFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.values.address.ValueAddressFragment
import minerva.android.values.create.NewValueFragment
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
        val fragment = when (fragmentType) {
            WrappedFragmentType.IDENTITY -> EditIdentityFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.VALUE -> NewValueFragment.newInstance(intent.getIntExtra(POSITION, Int.InvalidIndex))
            WrappedFragmentType.VALUE_ADDRESS -> ValueAddressFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
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
            WrappedFragmentType.VALUE -> getString(R.string.new_account)
            WrappedFragmentType.VALUE_ADDRESS -> String.Empty
        }

    private fun prepareActionBar(fragmentType: WrappedFragmentType) {
        supportActionBar?.apply {
            val actionBarTitle = intent.getStringExtra(TITLE) ?: getDefaultTitle(fragmentType)
            val logoRes = intent.getIntExtra(LOGO, Int.InvalidId)
            title = "  $actionBarTitle"  //hacks for padding between logo and title

            setDisplayHomeAsUpEnabled(true)
            if (logoRes != Int.InvalidId) {
                setDisplayShowHomeEnabled(true)
                setDisplayUseLogoEnabled(true)
                setLogo(getDrawable(logoRes))
            }
        }
    }

    private fun getFragmentType() = intent.getSerializableExtra(FRAGMENT) as WrappedFragmentType?
        ?: throw MissingFormatArgumentException("No fragment was passed to Activity")

    companion object {
        const val TITLE = "title"
        const val INDEX = "index"
        const val POSITION = "position"
        const val FRAGMENT = "fragment"
        const val LOGO = "logo"
    }
}

enum class WrappedFragmentType {
    IDENTITY,
    VALUE,
    VALUE_ADDRESS
}
