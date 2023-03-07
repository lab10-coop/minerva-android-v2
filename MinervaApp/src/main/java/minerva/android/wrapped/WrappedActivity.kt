package minerva.android.wrapped

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.accounts.address.AddressFragment
import minerva.android.accounts.akm.SafeAccountSettingsFragment
import minerva.android.accounts.create.NewAccountFragment
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.listener.OnBackListener
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.edit.EditOrderFragment
import minerva.android.extension.addFragment
import minerva.android.extension.addFragmentWithBackStack
import minerva.android.extension.getCurrentFragment
import minerva.android.identities.edit.EditIdentityFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.settings.advanced.AdvancedFragment
import minerva.android.settings.authentication.AuthenticationFragment
import minerva.android.settings.fiat.FiatFragment
import minerva.android.settings.version.AppVersionFragment
import minerva.android.token.AddTokenFragment
import minerva.android.token.ManageTokensFragment
import minerva.android.token.ramp.RampFragment
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.widget.repository.getNetworkIcon
import java.util.*

class WrappedActivity : AppCompatActivity(), AddressScannerListener, OnBackListener, ShowFragmentListener {

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
        getCurrentFragment()?.let {
            when (it) {
                is SafeAccountSettingsFragment -> it.setScanResult(text)
                is AddTokenFragment -> it.setScanResult(text)
            }
        }
    }

    override fun showScanner(scanner: BaseScannerFragment) {
        supportActionBar?.hide()
        addFragmentWithBackStack(
            R.id.container,
            scanner,
            R.animator.slide_in_left,
            R.animator.slide_out_right
        )
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
        getCurrentFragment()?.onResume()
    }

    override fun showFragment(fragment: Fragment, slideIn: Int, slideOut: Int, title: String?) {
        addFragmentWithBackStack(R.id.container, fragment, slideIn, slideOut)
        setActionBarTitle(title)
    }

    override fun setActionBarTitle(title: String?) {
        title?.let {
            supportActionBar?.title = String.format(TITLE_FORMAT, title)
        }
    }

    private fun prepareFragment(fragmentType: WrappedFragmentType) {
        val fragment = when (fragmentType) {
            WrappedFragmentType.IDENTITY -> EditIdentityFragment.newInstance(
                intent.getIntExtra(INDEX, Int.InvalidIndex),
                intent.getParcelableExtra(SERVICE_QR_CODE)
            )
            WrappedFragmentType.IDENTITY_ORDER -> EditOrderFragment.newInstance(WalletActionType.IDENTITY)
            WrappedFragmentType.ACCOUNT -> NewAccountFragment.newInstance()
            WrappedFragmentType.ACCOUNT_ADDRESS -> AddressFragment.newInstance(
                fragmentType,
                intent.getIntExtra(INDEX, Int.InvalidIndex)
            )
            WrappedFragmentType.IDENTITY_ADDRESS -> AddressFragment.newInstance(
                fragmentType,
                intent.getIntExtra(INDEX, Int.InvalidIndex)
            )
            WrappedFragmentType.ACCOUNT_ORDER -> EditOrderFragment.newInstance(WalletActionType.ACCOUNT)
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> SafeAccountSettingsFragment.newInstance(
                intent.getIntExtra(
                    INDEX,
                    Int.InvalidIndex
                )
            )
            WrappedFragmentType.SERVICE_ORDER -> EditOrderFragment.newInstance(WalletActionType.SERVICE)
            WrappedFragmentType.CREDENTIAL_ORDER -> EditOrderFragment.newInstance(WalletActionType.CREDENTIAL)
            WrappedFragmentType.MANAGE_TOKENS -> ManageTokensFragment.newInstance(intent.getIntExtra(INDEX, Int.InvalidIndex))
            WrappedFragmentType.AUTHENTICATION -> AuthenticationFragment.newInstance()
            WrappedFragmentType.RAMP -> RampFragment.newInstance()
            WrappedFragmentType.CURRENCY -> FiatFragment.newInstance()
            WrappedFragmentType.APP_VERSION -> AppVersionFragment.newInstance()
            WrappedFragmentType.ADVANCED -> AdvancedFragment.newInstance()
        }
        addFragment(R.id.container, fragment)
    }

    private fun getDefaultTitle(fragmentType: WrappedFragmentType) =
        when (fragmentType) {
            WrappedFragmentType.IDENTITY -> getString(R.string.new_identity)
            WrappedFragmentType.IDENTITY_ORDER -> getString(R.string.edit_identity_order)
            WrappedFragmentType.ACCOUNT -> getString(R.string.add_account)
            WrappedFragmentType.ACCOUNT_ADDRESS, WrappedFragmentType.IDENTITY_ADDRESS -> String.Empty
            WrappedFragmentType.ACCOUNT_ORDER -> getString(R.string.edit_account_order)
            WrappedFragmentType.SAFE_ACCOUNT_SETTINGS -> getString(R.string.settings)
            WrappedFragmentType.SERVICE_ORDER -> getString(R.string.edit_service_order)
            WrappedFragmentType.CREDENTIAL_ORDER -> getString(R.string.edit_credentials_order)
            WrappedFragmentType.MANAGE_TOKENS -> getString(R.string.manage_token)
            WrappedFragmentType.RAMP -> getString(R.string.buy_crypto)
            WrappedFragmentType.AUTHENTICATION -> getString(R.string.authentication)
            WrappedFragmentType.CURRENCY -> getString(R.string.currency)
            WrappedFragmentType.APP_VERSION -> getString(R.string.version)
            WrappedFragmentType.ADVANCED -> getString(R.string.advanced)
        }

    private fun prepareActionBar(fragmentType: WrappedFragmentType) {
        supportActionBar?.apply {
            (intent.getStringExtra(TITLE) ?: getDefaultTitle(fragmentType)).let {
                title = String.format(TITLE_FORMAT, it)
            }
            intent.getStringExtra(SUBTITLE)?.let {
                title = String.format(TITLE_FORMAT, it)
            }

            val isSafeAccount = intent.getBooleanExtra(IS_SAFE_ACCOUNT, false)

            setDisplayHomeAsUpEnabled(true)
            intent.getIntExtra(CHAIN_ID, Int.InvalidId).let { chainId ->
                if (chainId != Int.InvalidId) {
                    setDisplayShowHomeEnabled(true)
                    setDisplayUseLogoEnabled(true)
                    setLogo(getNetworkIcon(this@WrappedActivity, chainId, isSafeAccount))
                }
            }
        }
    }

    private fun getFragmentType() = intent.getSerializableExtra(FRAGMENT) as WrappedFragmentType?
        ?: throw MissingFormatArgumentException("No fragment was passed to Activity")

    companion object {
        const val TITLE_FORMAT = "%s"//(deleted)hacks for padding between logo and title
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val INDEX = "index"
        const val POSITION = "position"
        const val FRAGMENT = "fragment"
        const val CHAIN_ID = "chainId"
        const val ADDRESS = "address"
        const val FRAGMENT_TYPE = "type"
        const val SERVICE_QR_CODE = "service_qr_code"
        const val IS_SAFE_ACCOUNT = "is_safe_account"
        const val PRIVATE_KEY = "private_key"
    }
}

enum class WrappedFragmentType {
    IDENTITY,
    IDENTITY_ADDRESS,
    IDENTITY_ORDER,
    ACCOUNT,
    ACCOUNT_ADDRESS,
    ACCOUNT_ORDER,
    CREDENTIAL_ORDER,
    SAFE_ACCOUNT_SETTINGS,
    SERVICE_ORDER,
    MANAGE_TOKENS,
    AUTHENTICATION,
    RAMP,
    CURRENCY,
    APP_VERSION,
    ADVANCED
}
