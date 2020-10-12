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

object MinervaFlashBarWithTwoButtons {
    fun show(
        activity: Activity,
        message: String,
        positiveButton: Int,
        negativeButton: Int,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit = {},
        title: String = activity.getString(R.string.message)
    ) {
        getDefaultFlashBar(activity, title, message)
            .positiveActionTextSizeInSp(FONT_SIZE)
            .positiveActionTextColorRes(R.color.colorPrimary)
            .negativeActionTextSizeInSp(FONT_SIZE)
            .negativeActionTextColorRes(R.color.colorPrimary)
            .positiveActionText(activity.getString(positiveButton))
            .negativeActionText(activity.getString(negativeButton))
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    positiveAction()
                }
            })
            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    negativeAction()
                }
            })
            .build()
            .show()
    }
}

object MinervaFlashBarWithThreeButtons {
    fun show(
        activity: Activity,
        message: String,
        positiveButton: Int,
        primaryButton: Int,
        negativeButton: Int,
        positiveAction: () -> Unit,
        primaryAction: () -> Unit,
        negativeAction: () -> Unit = {},
        title: String = activity.getString(R.string.message)
    ) {
        getDefaultFlashBar(activity, title, message)
            .positiveActionTextSizeInSp(FONT_SIZE)
            .positiveActionTextColorRes(R.color.colorPrimary)
            .alternativeActionTextSizeInSp(FONT_SIZE)
            .alternativeActionTextColorRes(R.color.colorPrimary)
            .negativeActionTextSizeInSp(FONT_SIZE)
            .negativeActionTextColorRes(R.color.colorPrimary)
            .positiveActionText(activity.getString(positiveButton))
            .alternativeActionText(activity.getString(primaryButton))
            .negativeActionText(activity.getString(negativeButton))
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    positiveAction()
                }
            })
            .alternativeActionTapListener(object: Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    primaryAction()
                }
            })
            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    negativeAction()
                }
            })
            .build()
            .show()
    }
}

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
                .accelerateDecelerate()
        )
        .exitAnimation(
            FlashAnim.with(activity)
                .animateBar()
                .duration(EXIT_ANIM_DURATION)
                .accelerateDecelerate()
        )
        .backgroundColorRes(R.color.white)

const val FLASHBAR_DURATION = 30000L
const val ENTER_ANIM_DURATION = 750L
const val EXIT_ANIM_DURATION = 400L
const val FONT_SIZE = 12F