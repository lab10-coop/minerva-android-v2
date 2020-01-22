package minerva.android.main.handler

import android.app.Activity
import android.content.Intent
import minerva.android.R
import minerva.android.main.MainActivity
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.values.transaction.TransactionActivity
import minerva.android.widget.MinervaFlashbar

internal fun MainActivity.handleTransactionResult(data: Intent?) {
    data?.getBooleanExtra(TransactionActivity.IS_TRANSACTION_SUCCESS, false)?.let { isTransactionSuccess ->
        if (isTransactionSuccess) {
//            todo show amount transferred in flashbar
            MinervaFlashbar.show(
                this,
                getString(R.string.transaction_success_title),
                getString(R.string.transaction_success_message)
            )
        } else {
            MinervaFlashbar.show(
                this,
                getString(R.string.transaction_error_title),
                getString(R.string.transaction_error_message)
            )
        }
    }
}

internal fun MainActivity.handleLoginResult(data: Intent?) {
    data?.getBooleanExtra(PainlessLoginActivity.IS_LOGIN_SUCCESS, false)?.let { isLoginSuccess ->
        if (isLoginSuccess) {
            MinervaFlashbar.show(
                this,
                getString(R.string.login_success_title),
                getString(R.string.login_success_message)
            )
        } else {
            MinervaFlashbar.show(
                this,
                getString(R.string.login_failure_title),
                getString(R.string.login_failure_message)
            )
        }
    }
}

internal fun isTransactionResult(requestCode: Int, resultCode: Int) =
    requestCode == MainActivity.TRANSACTION_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK

internal fun isLoginResult(requestCode: Int, resultCode: Int) =
    requestCode == MainActivity.LOGIN_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK