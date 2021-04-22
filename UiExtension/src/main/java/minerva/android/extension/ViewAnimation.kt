package minerva.android.extension

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AnimationUtils

fun View.rotate180() {
    ObjectAnimator.ofFloat(this, ROTATE_ANIMATION, ROTATE_0_ANGLE, ROTATE_180_ANGLE).apply {
        duration = resources.getInteger(R.integer.rotate_animation_duration).toLong()
        start()
    }
}

fun View.rotate180back() {
    ObjectAnimator.ofFloat(this, ROTATE_ANIMATION, ROTATE_180_ANGLE, ROTATE_0_ANGLE).apply {
        with(resources) {
            duration = getInteger(R.integer.rotate_animation_duration).toLong()
            startDelay = getInteger(R.integer.rotate_animation_delay).toLong()
        }
        start()
    }
}

fun View.fadeIn() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
}

private const val ROTATE_180_ANGLE = 180f
private const val ROTATE_0_ANGLE = 0f
private const val ROTATE_ANIMATION = "rotation"