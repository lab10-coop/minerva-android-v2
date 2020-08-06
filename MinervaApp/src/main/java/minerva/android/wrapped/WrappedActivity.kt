package minerva.android.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.edit.EditOrderFragment
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.replaceFragment
import minerva.android.identities.edit.EditIdentityFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.accounts.address.AddressFragment
import minerva.android.accounts.akm.SafeAccountSettingsFragment
import minerva.android.accounts.create.NewAccountFragment
import minerva.android.accounts.listener.OnBackListener
import minerva.android.accounts.listener.ScannerFragmentsListener
import minerva.android.accounts.transaction.fragment.scanner.AddressScannerFragment
import minerva.android.walletmanager.model.defs.WalletActionType
import java.util.*

class WrappedActivity : AppCompatActivity(), ScannerFragmentsListener, OnBackListener {

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
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun setScanResult(text: String?) {
        onBack()
        (getCurrentFragment() as? SafeAccountSettingsFragment)?.setScanResult(text)
    }

    override fun showScanner() {
        supportActionBar?.hide()
        replaceFragment(R.id.container, AddressScannerFragment.newInstance(), R.animator.slide_in_left, R.animator.slide_out_right)
    }

    override fun onBackPressed() {
        getCurrentFragment()?.let {
            when (it) {
                is EditOrderFragment -> it.saveNewOrder()
                else -> onBack()
            }
        }
    }

    override fun onBack() {
        super.onBackPressed()
        supportActionBar?.show()
    }

    private fun prepareFragment(fragmentType: WrappedFragmentType) {
        val fragment = when (fragmentType) {
            WrappedFragmentType.IDENTITY -> EditIdentityFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.IDENTITY_ORDER -> EditOrderFragment.newInstance(WalletActionType.IDENTITY)
            WrappedFragmentType.ACCOUNT -> NewAccountFragment.newInstance(intent.getIntExtra(POSITION, Int.InvalidIndex))
            WrappedFragmentType.ACCOUNT_ADDRESS -> AddressFragment.newInstance(fragmentType, intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.IDENTITY_ADDRESS -> AddressFragment.newInstance(fragmentType, intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.ACCOUNT_ORDER -> EditOrderFragment.newInstance(WalletActionType.ACCOUNT)
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> SafeAccountSettingsFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.SERVICE_ORDER -> EditOrderFragment.newInstance(WalletActionType.SERVICE)
        }
        addFragment(R.id.container, fragment)
    }

    private fun getDefaultTitle(fragmentType: WrappedFragmentType) =
        when (fragmentType) {
            WrappedFragmentType.IDENTITY -> getString(R.string.new_identity)
            WrappedFragmentType.IDENTITY_ORDER -> getString(R.string.edit_identity_order)
            WrappedFragmentType.ACCOUNT -> getString(R.string.new_account)
            WrappedFragmentType.ACCOUNT_ADDRESS, WrappedFragmentType.IDENTITY_ADDRESS -> String.Empty
            WrappedFragmentType.ACCOUNT_ORDER -> getString(R.string.edit_account_order)
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> getString(R.string.settings)
            WrappedFragmentType.SERVICE_ORDER -> getString(R.string.edit_service_order)
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
        const val FRAGMENT_TYPE = "type"
    }
}

enum class WrappedFragmentType {
    IDENTITY,
    IDENTITY_ADDRESS,
    IDENTITY_ORDER,
    ACCOUNT,
    ACCOUNT_ADDRESS,
    ACCOUNT_ORDER,
    SAFE_ACCOUNT_SETTINGS,
    SERVICE_ORDER
}
