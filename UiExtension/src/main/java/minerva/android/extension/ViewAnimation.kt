package minerva.android.extension

import android.view.View
import android.view.animation.AnimationUtils

fun View.rotate180() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180))
}

fun View.rotate180back() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180_back))
}

fun View.fadeIn() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
}