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
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.main.handler.isIdentityPrepared
import minerva.android.services.login.identity.ChooseIdentityFragment
import minerva.android.services.login.scanner.LoginScannerFragment
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import minerva.android.wrapped.WrappedActivity.Companion.SERVICE_QR_CODE

class LoginScannerActivity : AppCompatActivity(), LoginScannerListener {

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

    override fun onScannerResult(isResultSucceed: Boolean, message: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(IS_RESULT_SUCCEED, isResultSucceed)
            putExtra(RESULT_MESSAGE, message)
        })
        finish()
    }

    override fun onPainlessLoginResult(isLoginSucceed: Boolean, payload: LoginPayload?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(IS_RESULT_SUCCEED, isLoginSucceed)
            putExtra(LOGIN_PAYLOAD, payload)
        })
        finish()
    }

    override fun updateBindedCredential(qrCode: CredentialQrCode) {
        setResult(Activity.RESULT_OK, Intent().putExtra(CREDENTIAL_QR_CODE, qrCode))
        finish()
    }

    private fun isChooseIdentityFragment() = getCurrentFragment() is ChooseIdentityFragment

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isChooseIdentityFragment() && isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            isIdentityPrepared(requestCode, resultCode) -> {
                data?.getIntExtra(INDEX, Int.InvalidIndex)?.let { index ->
                    data.getParcelableExtra<ServiceQrCode>(SERVICE_QR_CODE)?.let { serviceQrCode ->
                        (getCurrentFragment() as? ChooseIdentityFragment)?.handleLogin(index, serviceQrCode)
                    }
                }
            }
        }
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    override fun showChooseIdentityFragment(qrCode: ServiceQrCode) {
        showFragment(qrCode)
        setupActionBar()
    }

    private fun showFragment(qrCodeCodeResponse: ServiceQrCode) {
        replaceFragment(
            R.id.container, ChooseIdentityFragment.newInstance(qrCodeCodeResponse),
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
        const val IS_RESULT_SUCCEED = "is_result_succeed"
        const val LOGIN_PAYLOAD = "login_payload"
        const val RESULT_MESSAGE = "result_message"
        const val CREDENTIAL_QR_CODE = "credential_qr_code"
    }
}
