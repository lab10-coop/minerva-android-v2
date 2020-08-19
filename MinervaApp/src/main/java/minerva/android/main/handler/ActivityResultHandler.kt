package minerva.android.main.handler

import android.app.Activity
import android.content.Intent
import minerva.android.R
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.MainActivity
import minerva.android.main.MainActivity.Companion.LOGIN_SCANNER_RESULT_REQUEST_CODE
import minerva.android.main.MainActivity.Companion.TRANSACTION_RESULT_REQUEST_CODE
import minerva.android.services.login.LoginScannerActivity
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.mappers.CredentialQrCodeResponseMapper
import minerva.android.widget.MinervaFlashBarWithButtons
import minerva.android.widget.MinervaFlashbar

internal fun MainActivity.handleTransactionResult(data: Intent?) {
    data?.apply {
        val isTransactionSuccess = getBooleanExtra(TransactionActivity.IS_TRANSACTION_SUCCESS, false)
        val transactionMessage = getStringExtra(TransactionActivity.TRANSACTION_MESSAGE)

        if (isTransactionSuccess) {
            MinervaFlashbar.show(
                this@handleTransactionResult,
                getString(R.string.transaction_success_title),
                getString(R.string.transaction_success_message, transactionMessage)
            )
        } else {
            MinervaFlashbar.show(
                this@handleTransactionResult,
                getString(R.string.transaction_error_title),
                getString(R.string.transaction_error_message, transactionMessage)
            )
        }
    }
}

internal fun MainActivity.handleLoginScannerResult(data: Intent?) {
    data?.let { intent ->
        (intent.getParcelableExtra(LoginScannerActivity.LOGIN_PAYLOAD) as? LoginPayload)?.let {
            handleServiceLogin(it, intent)
        }.orElse {
            handleCredentialLogin(intent)
        }
    }
}

private fun MainActivity.handleServiceLogin(loginPayload: LoginPayload, intent: Intent) {
    viewModel.loginPayload = loginPayload
    if (intent.getBooleanExtra(LoginScannerActivity.IS_RESULT_SUCCEED, false)) handleSuccessLoginStatuses(loginPayload.loginStatus)
    else handleLoginStatuses(loginPayload.loginStatus)
}

private fun MainActivity.handleCredentialLogin(intent: Intent) {
    (intent.getParcelableExtra(LoginScannerActivity.CREDENTIAL_QR_CODE) as? CredentialQrCode)?.let {
        viewModel.credential = CredentialQrCodeResponseMapper.map(it)
        MinervaFlashBarWithButtons.show(
            this,
            getString(R.string.update_credential_message),
            R.string.yes,
            R.string.cancel,
            { this.updateCredential() })
    }.orElse {
        showBindCredentialFlashbar(
            intent.getBooleanExtra(LoginScannerActivity.IS_RESULT_SUCCEED, false),
            intent.getStringExtra(LoginScannerActivity.RESULT_MESSAGE)
        )
    }
}

fun MainActivity.showBindCredentialFlashbar(isLoginSuccess: Boolean, message: String?) {
    if (isLoginSuccess) MinervaFlashbar.show(this, getString(R.string.success), getString(R.string.attached_credential_success, message))
    else message?.let { MinervaFlashbar.show(this, getString(R.string.error_importing_credential), it) }
        .orElse {
            MinervaFlashbar.show(this, getString(R.string.error_importing_credential), getString(R.string.unexpected_error))
        }
}

fun MainActivity.handleLoginStatuses(loginAction: Int) {
    when (loginAction) {
        KNOWN_USER -> showKnownUserFlashbar()
        KNOWN_QUICK_USER -> showQuickUserFlashbar(true)
        else -> MinervaFlashbar.show(this, getString(R.string.login_failure_title), getString(R.string.login_failure_message))
    }
}

private fun MainActivity.showKnownUserFlashbar() {
    MinervaFlashBarWithButtons.show(
        this,
        getString(R.string.known_user_login_message, viewModel.getIdentityName()),
        R.string.login,
        R.string.cancel,
        { this.onPainlessLogin() }
    )
}

private fun MainActivity.handleSuccessLoginStatuses(loginAction: Int) {
    when (loginAction) {
        NEW_USER -> MinervaFlashbar.show(this, getString(R.string.login_success_title), getString(R.string.login_success_message))
        NEW_QUICK_USER -> showQuickUserFlashbar(false)
    }
}

private fun MainActivity.showQuickUserFlashbar(shouldLogin: Boolean) {
    MinervaFlashBarWithButtons.show(
        this,
        getString(R.string.quick_login_success_message),
        R.string.allow,
        R.string.deny,
        { this.onAllowNotifications(shouldLogin) },
        { this.onDenyNotifications() },
        getString(R.string.login_success_title)
    )
}

internal fun isTransactionPrepared(requestCode: Int, resultCode: Int) =
    requestCode == TRANSACTION_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK

internal fun isLoginScannerResult(requestCode: Int, resultCode: Int) =
    requestCode == LOGIN_SCANNER_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK