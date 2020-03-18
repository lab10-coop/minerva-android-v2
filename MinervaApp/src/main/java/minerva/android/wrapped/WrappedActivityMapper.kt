package minerva.android.wrapped

import android.content.Context
import minerva.android.extension.launchActivity

fun startNewIdentityWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
    }
}

fun startEditIdentityWrappedActivity(context: Context, position: Int, title: String) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
    }
}

fun startValueAddressWrappedActivity(context: Context, title: String, position: Int, logoRes: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.VALUE_ADDRESS)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
        putExtra(WrappedActivity.LOGO, logoRes)
    }
}

fun startNewValueWrappedActivity(context: Context, title: String, position: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.VALUE)
        putExtra(WrappedActivity.TITLE, title)
        putExtra(WrappedActivity.POSITION, position)
    }
}

fun startSafeAccountWrappedActivity(context: Context, subtitle: String, position: Int, logoRes: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.SAFE_ACCOUNT_SETTINGS)
        putExtra(WrappedActivity.SUBTITLE, subtitle)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.LOGO, logoRes)
    }
}