package minerva.android.widget

import android.app.Activity
import android.widget.ImageView
import com.andrognito.flashbar.Flashbar
import com.andrognito.flashbar.anim.FlashAnim
import minerva.android.R

object MinervaFlashbar {
    fun show(activity: Activity, title: String, message: String) {
        Flashbar.Builder(activity)
            .gravity(Flashbar.Gravity.TOP)
            .title(title)
            .titleColorRes(R.color.black)
            .titleSizeInSp(FONT_SIZE)
            .message(message)
            .messageColorRes(R.color.black)
            .messageSizeInSp(FONT_SIZE)
            .castShadow(true, 1)
            .primaryActionText(activity.getString(R.string.dissmiss))
            .primaryActionTextSizeInSp(FONT_SIZE)
            .primaryActionTextColorRes(R.color.colorPrimary)
            .primaryActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                }

            })
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
            .duration(FLASHBAR_DURATION)
            .build()
            .show()

    }
}

const val FLASHBAR_DURATION = 5000L
const val ENTER_ANIM_DURATION = 750L
const val EXIT_ANIM_DURATION = 400L
const val FONT_SIZE = 12F