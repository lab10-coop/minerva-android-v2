package minerva.android.widget

import android.app.Activity
import android.widget.ImageView
import com.andrognito.flashbar.Flashbar
import com.andrognito.flashbar.anim.FlashAnim
import minerva.android.R

object MinervaFlashbar {
    fun show(activity: Activity, title: String, message: String) {
        getDefaultFlashBar(activity, title, message)
            .duration(FLASHBAR_DURATION)
            .primaryActionText(activity.getString(R.string.dismiss))
            .primaryActionTextSizeInSp(FONT_SIZE)
            .primaryActionTextColorRes(R.color.colorPrimary)
            .primaryActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                }
            })
            .build()
            .show()

    }
}

object KnownUserLoginFlashBar {
    fun show(activity: Activity, message: String, listener: OnFlashBarTapListener, title: String = activity.getString(R.string.message)) {
        prepareFlashBarWithButtons(activity, title, message, R.string.login, R.string.cancel)
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    listener.onLogin()
                }
            })
            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                }
            })
            .build()
            .show()
    }
}

object QuickLoginFlashBar {
    fun show(
        activity: Activity,
        message: String,
        listener: OnFlashBarTapListener,
        title: String = activity.getString(R.string.message),
        shouldLogin: Boolean
    ) {
        prepareFlashBarWithButtons(activity, title, message, R.string.allow, R.string.deny)
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    listener.onAllow(shouldLogin)
                }
            })
            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    listener.onDeny()
                }
            })
            .build()
            .show()
    }
}

private fun prepareFlashBarWithButtons(
    activity: Activity,
    title: String,
    message: String,
    positiveButtonId: Int,
    negativeButtonId: Int
): Flashbar.Builder =
    getDefaultFlashBar(activity, title, message)
        .positiveActionTextSizeInSp(FONT_SIZE)
        .positiveActionTextColorRes(R.color.colorPrimary)
        .negativeActionTextSizeInSp(FONT_SIZE)
        .negativeActionTextColorRes(R.color.colorPrimary)
        .positiveActionText(activity.getString(positiveButtonId))
        .negativeActionText(activity.getString(negativeButtonId))

private fun getDefaultFlashBar(activity: Activity, title: String, message: String): Flashbar.Builder =
    Flashbar.Builder(activity)
        .gravity(Flashbar.Gravity.TOP)
        .title(title)
        .titleColorRes(R.color.black)
        .titleSizeInSp(FONT_SIZE)
        .message(message)
        .messageColorRes(R.color.black)
        .messageSizeInSp(FONT_SIZE)
        .castShadow(true, 1)
        .showIcon(1f, ImageView.ScaleType.CENTER_CROP)
        .icon(R.drawable.ic_minerva_icon)
        .iconColorFilterRes(android.R.color.transparent)
        .enterAnimation(
            FlashAnim.with(activity)
                .animateBar()
                .duration(ENTER_ANIM_DURATION)
                .alpha()
                .overshoot()
        )
        .exitAnimation(
            FlashAnim.with(activity)
                .animateBar()
                .duration(EXIT_ANIM_DURATION)
                .accelerateDecelerate()
        )
        .backgroundColorRes(R.color.white)

interface OnFlashBarTapListener {
    fun onAllow(shouldLogin: Boolean)
    fun onDeny()
    fun onLogin()
}

const val FLASHBAR_DURATION = 8000L
const val ENTER_ANIM_DURATION = 750L
const val EXIT_ANIM_DURATION = 400L
const val FONT_SIZE = 12F