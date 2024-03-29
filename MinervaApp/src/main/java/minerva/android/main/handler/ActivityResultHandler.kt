package minerva.android.main.handler

import android.app.Activity
import android.content.Intent
import minerva.android.R
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.MainActivity
import minerva.android.main.MainActivity.Companion.EDIT_IDENTITY_RESULT_REQUEST_CODE
import minerva.android.main.MainActivity.Companion.SERVICES_SCANNER_RESULT_REQUEST_CODE
import minerva.android.main.MainActivity.Companion.TRANSACTION_RESULT_REQUEST_CODE
import minerva.android.services.login.ServicesScannerActivity
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.BACKUP_FAILURE
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.widget.MinervaFlashBarWithThreeButtons
import minerva.android.widget.MinervaFlashBarWithTwoButtons
import minerva.android.widget.MinervaFlashbar

internal fun MainActivity.handleLoginScannerResult(data: Intent?) {
    data?.let { intent ->
        (intent.getParcelableExtra(ServicesScannerActivity.LOGIN_PAYLOAD) as? LoginPayload)?.let {
            handleServiceLogin(it, intent)
        }.orElse {
            handleServiceScannerResult(intent)
        }
    }
}

private fun MainActivity.handleServiceLogin(loginPayload: LoginPayload, intent: Intent) {
    viewModel.loginPayload = loginPayload
    if (intent.getBooleanExtra(
            ServicesScannerActivity.IS_RESULT_SUCCEED,
            false
        )
    ) handleSuccessLoginStatuses(loginPayload.loginStatus)
    else handleLoginStatuses(loginPayload.loginStatus)
}

private fun MainActivity.handleServiceScannerResult(intent: Intent) {
    val qrCode = intent.getParcelableExtra<CredentialQrCode>(ServicesScannerActivity.CREDENTIAL_QR_CODE)
    val isWalletConnectResult = intent.getBooleanExtra(ServicesScannerActivity.IS_WALLET_CONNECT_RESULT, false)
    when {
        qrCode != null -> handleCredentialQrCode(qrCode)
        isWalletConnectResult -> goToValuesTab()
        else -> {
            showBindCredentialFlashbar(
                intent.getBooleanExtra(ServicesScannerActivity.IS_RESULT_SUCCEED, false),
                intent.getStringExtra(ServicesScannerActivity.RESULT_MESSAGE)
            )
        }
    }
}

fun MainActivity.handleCredentialQrCode(qrCode: CredentialQrCode) {
    viewModel.qrCode = qrCode
    MinervaFlashBarWithThreeButtons.show(
        this,
        getString(R.string.update_credential_message),
        viewModel.getReplaceLabelRes(qrCode),
        R.string.add_as_new,
        R.string.cancel,
        { viewModel.updateBindedCredentials(true) },
        { viewModel.updateBindedCredentials(false) }
    )
}

fun MainActivity.showBindCredentialFlashbar(isLoginSuccess: Boolean, message: String?) {
    when {
        isLoginSuccess && !message.isNullOrEmpty() -> MinervaFlashbar.show(
            this,
            getString(R.string.success),
            getString(R.string.attached_credential_success, message)
        )
        !isLoginSuccess && !message.isNullOrEmpty() -> MinervaFlashbar.show(this, getString(R.string.auth_error_title), message)
        !isLoginSuccess ->
            MinervaFlashbar.show(this, getString(R.string.error_title), getString(R.string.unexpected_error))
    }
}

fun MainActivity.handleLoginStatuses(status: Int) {
    when (status) {
        KNOWN_USER -> showKnownUserFlashbar()
        KNOWN_QUICK_USER -> showQuickUserFlashbar(true)
        BACKUP_FAILURE -> MinervaFlashbar.show(
            this,
            getString(R.string.login_failure_title),
            getString(R.string.automatic_backup_failed_error)
        )
        else -> MinervaFlashbar.show(this, getString(R.string.login_failure_title), getString(R.string.login_failure_message))
    }
}

private fun MainActivity.showKnownUserFlashbar() {
    MinervaFlashBarWithTwoButtons.show(
        this,
        getString(R.string.known_user_login_message, viewModel.getIdentityName()),
        R.string.login,
        R.string.cancel,
        { this.onPainlessLogin() }
    )
}

private fun MainActivity.handleSuccessLoginStatuses(loginAction: Int) {
    when (loginAction) {
        NEW_USER -> showSuccessFlashbar()
        NEW_QUICK_USER -> showQuickUserFlashbar(false)
    }
}

private fun MainActivity.showSuccessFlashbar() {
    //TODO showing Flashbar according to service name looks poor. Refactor?
    if (viewModel.loginPayload.qrCode?.serviceName?.isNotEmpty() == true)
        MinervaFlashbar.show(
            this,
            getString(R.string.login_success_title),
            getString(R.string.login_success_message)
        )
}

private fun MainActivity.showQuickUserFlashbar(shouldLogin: Boolean) {
    MinervaFlashBarWithTwoButtons.show(
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
    requestCode == SERVICES_SCANNER_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK

internal fun isIdentityPrepared(requestCode: Int, resultCode: Int) =
    requestCode == EDIT_IDENTITY_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK