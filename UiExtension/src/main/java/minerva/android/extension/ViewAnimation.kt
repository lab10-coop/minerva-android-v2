package minerva.android.extension

import android.view.View
import android.view.animation.AnimationUtils

fun View.rotate180() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180))
}

fun View.rotate180back() {
    visible()
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_180_back))
}