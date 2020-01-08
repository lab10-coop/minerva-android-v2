package minerva.wrapped

import android.content.Context
import minerva.android.extension.launchActivity

fun startNewIdentityWrappedActivity(context: Context) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
    }
}

fun startEditIdentityWrappedActivity(context: Context, index: Int, title: String) {
    context.launchActivity<WrappedActivity> {
        putExtra(WrappedActivity.FRAGMENT, WrappedFragmentType.IDENTITY)
        putExtra(WrappedActivity.INDEX, index)
        putExtra(WrappedActivity.TITLE, title)
    }
}