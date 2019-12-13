package minerva.android.extension

import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible

fun View.slideOutToLeft() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_to_left))
    gone()
}

fun View.slideInFromRight() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_from_right))
}

fun View.slideOutToRight() {
    if(this.isVisible) {
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_to_right))
        gone()
    }
}

fun View.slideInFromLeft() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_from_left))
}

fun View.rotate180() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180))
}

fun View.rotate180back() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180_back))
}