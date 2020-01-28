package minerva.android.services.login

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import minerva.android.R
import minerva.android.extension.getCurrentFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.identity.ChooseIdentityFragment
import minerva.android.services.login.scanner.LoginScannerFragment
import minerva.android.walletmanager.model.QrCodeResponse

class PainlessLoginActivity : AppCompatActivity(), PainlessLoginFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painless_login)
        hideToolbar()
        showScannerFragment()
        window.statusBarColor = getColor(R.color.lightGray)
    }

    private fun showScannerFragment() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.loginFragmentsContainer, LoginScannerFragment.newInstance())
            commit()
        }
    }

    override fun onBackPressed() {
        hideToolbar()
        super.onBackPressed()
    }

    private fun hideToolbar() {
        supportActionBar?.hide()
    }

    override fun onResult(isResultSucceed: Boolean, message: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(IS_LOGIN_SUCCESS, isResultSucceed))
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
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.animator.slide_in_left, 0, 0, R.animator.slide_out_right)
            replace(R.id.loginFragmentsContainer, ChooseIdentityFragment.newInstance(qrCodeResponse))
            addToBackStack(null)
            commit()
        }
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
    }
}
