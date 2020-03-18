package minerva.android.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.R
import minerva.android.identities.edit.EditIdentityFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.values.address.ValueAddressFragment
import minerva.android.values.akm.AddressScannerFragment
import minerva.android.values.akm.AddressScannerFragment.Companion.SCANNER_FRAGMENT
import minerva.android.values.akm.SafeAccountSettingsFragment
import minerva.android.values.create.NewValueFragment
import java.util.*

class WrappedActivity : AppCompatActivity(), WrappedActivityListener {

    private val _extraStringLiveData = MutableLiveData<Event<String>>()
    override val extraStringLiveData: LiveData<Event<String>> get() = _extraStringLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrapped)

        val fragmentType = getFragmentType()
        prepareActionBar(fragmentType)
        prepareFragment(fragmentType)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun putStringExtra(string: String) {
        _extraStringLiveData.value = Event(string)
    }

    override fun goBack(fragment: Fragment) {
        supportActionBar?.show()
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_right)
            .remove(fragment)
            .commit()
        supportFragmentManager.popBackStack()

    }

    override fun showScanner() {
        supportActionBar?.hide()
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_right)
            .add(R.id.fragmentContainer, AddressScannerFragment.newInstance())
            .addToBackStack(SCANNER_FRAGMENT).commit()
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
            //else fragments
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
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
