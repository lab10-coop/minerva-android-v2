package minerva.android.main.handler

import android.app.Activity
import android.content.Intent
import minerva.android.R
import minerva.android.main.MainActivity
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.values.transaction.activity.TransactionActivity
import minerva.android.widget.KnownUserLoginFlashBar
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.QuickLoginFlashBar

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

internal fun MainActivity.handleLoginResult(data: Intent?) {
    data?.apply {
        val isLoginSuccess = getBooleanExtra(PainlessLoginActivity.IS_LOGIN_SUCCESS, false)
        (getParcelableExtra(PainlessLoginActivity.LOGIN_PAYLOAD) as LoginPayload).run {
            viewModel.loginPayload = this
            if (isLoginSuccess) handleSuccessLoginStatuses(loginStatus)
            else handleLoginStatuses(loginStatus)
        }
    }
}

fun MainActivity.handleLoginStatuses(loginAction: Int) {
    when (loginAction) {
        KNOWN_USER -> KnownUserLoginFlashBar.show(this, getString(R.string.known_user_login_message, viewModel.getIdentityName()), this)
        KNOWN_QUICK_USER -> QuickLoginFlashBar.show(this, getString(R.string.quick_login_success_message), this, shouldLogin = true)
        else -> MinervaFlashbar.show(this, getString(R.string.login_failure_title), getString(R.string.login_failure_message))
    }
}

private fun MainActivity.handleSuccessLoginStatuses(loginAction: Int) {
    when (loginAction) {
        NEW_USER -> MinervaFlashbar.show(this, getString(R.string.login_success_title), getString(R.string.login_success_message))
        NEW_QUICK_USER -> QuickLoginFlashBar.show(
            this, getString(R.string.quick_login_success_message),
            this, getString(R.string.login_success_title),
            false
        )
    }
}

internal fun isTransactionPrepared(requestCode: Int, resultCode: Int) =
    requestCode == MainActivity.TRANSACTION_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK

internal fun isLoginResult(requestCode: Int, resultCode: Int) =
    requestCode == MainActivity.LOGIN_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK