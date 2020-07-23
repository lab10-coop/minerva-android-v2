package minerva.android.services.login

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.replaceFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.identity.ChooseIdentityFragment
import minerva.android.services.login.scanner.LoginScannerFragment
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.model.QrCodeResponse

class PainlessLoginActivity : AppCompatActivity(), PainlessLoginFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painless_login)
        hideToolbar()
        addFragment(R.id.container, LoginScannerFragment.newInstance())
        window.statusBarColor = getColor(R.color.lightGray)
    }

    override fun onBackPressed() {
        hideToolbar()
        super.onBackPressed()
    }

    private fun hideToolbar() {
        supportActionBar?.hide()
    }

    override fun onResult(isResultSucceed: Boolean, message: String?, loginPayload: LoginPayload?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(IS_LOGIN_SUCCESS, isResultSucceed)
            putExtra(LOGIN_PAYLOAD, loginPayload)
        })
        finish()
    }

    private fun isChooseIdentityFragment() = getCurrentFragment() is ChooseIdentityFragment

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isChooseIdentityFragment() && isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    override fun showChooseIdentityFragment(qrCodeResponse: QrCodeResponse) {
        showFragment(qrCodeResponse)
        setupActionBar()
    }

    private fun showFragment(qrCodeResponse: QrCodeResponse) {
        replaceFragment(
            R.id.container, ChooseIdentityFragment.newInstance(qrCodeResponse),
            R.animator.slide_in_left, R.animator.slide_out_right
        )
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            show()
            title = String.Empty
            setBackgroundDrawable(ColorDrawable(getColor(R.color.lightGray)))
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    companion object {
        const val IS_LOGIN_SUCCESS = "is_login_succeed"
        const val LOGIN_PAYLOAD = "login_payload"
    }
}
