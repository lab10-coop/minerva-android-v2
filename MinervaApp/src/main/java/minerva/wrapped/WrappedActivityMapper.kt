package minerva.wrapped

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

fun startValueAddressWrappedActivity(context: Context, position: Int, title: String, logoRes: Int) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.VALUE_ADDRESS)
        putExtra(WrappedActivity.INDEX, position)
        putExtra(WrappedActivity.TITLE, title)
        putExtra(WrappedActivity.LOGO, logoRes)
    }
}