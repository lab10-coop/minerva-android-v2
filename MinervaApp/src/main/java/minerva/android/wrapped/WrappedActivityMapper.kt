package minerva.android.wrapped

import android.app.Activity
import android.content.Context
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.extension.launchActivity
import minerva.android.extension.launchActivityForResult
import minerva.android.main.MainActivity.Companion.EDIT_IDENTITY_RESULT_REQUEST_CODE
import minerva.android.walletmanager.model.ServiceQrCode

fun startNewIdentityWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
    }
}

fun startNewIdentityOnResultWrappedActivity(activity: Activity?, serviceQrCode: ServiceQrCode? = null) {
    activity?.launchActivityForResult<WrappedActivity>(EDIT_IDENTITY_RESULT_REQUEST_CODE) {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
        serviceQrCode?.let {
            putExtra(WrappedActivity.SERVICE_QR_CODE, serviceQrCode)
        }
    }
}

fun startEditIdentityOnResultWrappedActivity(
    activity: Activity?,
    position: Int,
    title: String,
    serviceQrCode: ServiceQrCode?
) {
    activity?.launchActivityForResult<WrappedActivity>(EDIT_IDENTITY_RESULT_REQUEST_CODE) {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
        serviceQrCode?.let {
            putExtra(WrappedActivity.SERVICE_QR_CODE, serviceQrCode)
        }
    }
}

fun startEditIdentityOrderWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY_ORDER)
    }
}

fun startEditIdentityWrappedActivity(context: Context, position: Int, title: String) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
    }
}

fun startIdentityAddressWrappedActivity(context: Context, title: String, position: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY_ADDRESS)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
    }
}

fun startAccountAddressWrappedActivity(
    context: Context,
    title: String,
    index: Int,
    networkShort: String,
    isSafeAccount: Boolean
) {
    context.launchActivity<TransactionActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.ACCOUNT_ADDRESS)
        putExtra(WrappedActivity.INDEX, index)
        putExtra(WrappedActivity.TITLE, title)
        putExtra(WrappedActivity.NETWORK, networkShort)
        putExtra(WrappedActivity.IS_SAFE_ACCOUNT, isSafeAccount)
    }
}

fun startNewAccountWrappedActivity(context: Context, title: String) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.ACCOUNT)
        putExtra(WrappedActivity.TITLE, title)
    }
}

fun startEditAccountOrderWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.ACCOUNT_ORDER)
    }
}

fun startEditCredentialOrderWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.CREDENTIAL_ORDER)
    }
}

fun startSafeAccountWrappedActivity(
    context: Context,
    subtitle: String,
    position: Int,
    chainId: Int,
    isSafeAccount: Boolean
) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.SAFE_ACCOUNT_SETTINGS)
        putExtra(WrappedActivity.SUBTITLE, subtitle)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.NETWORK, chainId)
        putExtra(WrappedActivity.IS_SAFE_ACCOUNT, isSafeAccount)
    }
}

fun startEditServiceOrderWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.SERVICE_ORDER)
    }
}

fun startManageAssetsWrappedActivity(context: Context, index: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.MANAGE_ASSETS)
        putExtra(WrappedActivity.INDEX, index)
    }
}