package minerva.android.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.replaceFragment
import minerva.android.identities.edit.EditIdentityFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.values.address.ValueAddressFragment
import minerva.android.values.akm.SafeAccountSettingsFragment
import minerva.android.values.create.NewValueFragment
import minerva.android.values.listener.ScannerFragmentsListener
import minerva.android.values.transaction.fragment.scanner.AddressScannerFragment
import java.util.*

class WrappedActivity : AppCompatActivity(), ScannerFragmentsListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrapped)
        getFragmentType().apply {
            prepareActionBar(this)
            prepareFragment(this)
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

    override fun setScanResult(text: String?) {
        onBackPressed()
        (getCurrentFragment() as? SafeAccountSettingsFragment)?.setScanResult(text)
    }

    override fun showScanner() {
        supportActionBar?.hide()
        replaceFragment(R.id.container, AddressScannerFragment.newInstance(), R.animator.slide_in_left, R.animator.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportActionBar?.show()
    }

    private fun prepareFragment(fragmentType: WrappedFragmentType) {
        val fragment = when (fragmentType) {
            WrappedFragmentType.IDENTITY -> EditIdentityFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.VALUE -> NewValueFragment.newInstance(intent.getIntExtra(POSITION, Int.InvalidIndex))
            WrappedFragmentType.VALUE_ADDRESS -> ValueAddressFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> SafeAccountSettingsFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
        }
        addFragment(R.id.container, fragment)
    }

    private fun getDefaultTitle(fragmentType: WrappedFragmentType) =
        when (fragmentType) {
            WrappedFragmentType.IDENTITY -> getString(R.string.new_identity)
            WrappedFragmentType.VALUE -> getString(R.string.new_account)
            WrappedFragmentType.VALUE_ADDRESS -> String.Empty
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> getString(R.string.settings)
        }

    private fun prepareActionBar(fragmentType: WrappedFragmentType) {
        supportActionBar?.apply {
            (intent.getStringExtra(TITLE) ?: getDefaultTitle(fragmentType)).apply {
                title = "  $this"  //hacks for padding between logo and title
            }
            intent.getStringExtra(SUBTITLE)?.let {
                subtitle = "   $it" //hacks for padding between logo and title
            }

            val logoRes = intent.getIntExtra(LOGO, Int.InvalidId)
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
        const val SUBTITLE = "subtitle"
        const val INDEX = "index"
        const val POSITION = "position"
        const val FRAGMENT = "fragment"
        const val LOGO = "logo"
    }
}

enum class WrappedFragmentType {
    IDENTITY,
    VALUE,
    VALUE_ADDRESS,
    SAFE_ACCOUNT_SETTINGS
}
