package minerva.android.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.addFragmentWithBackStack(containerId: Int, fragment: Fragment, slideIn: Int = 0, slideOut: Int = 0) =
    showFragment(containerId, fragment, slideIn, slideOut, replace = false, addToBackstack = true)

fun AppCompatActivity.addFragment(containerId: Int, fragment: Fragment, slideIn: Int = 0, slideOut: Int = 0) =
    showFragment(containerId, fragment, slideIn, slideOut, replace = false, addToBackstack = false)

fun AppCompatActivity.replaceFragmentWithBackStack(containerId: Int, fragment: Fragment, slideIn: Int = 0, slideOut: Int = 0) =
    showFragment(containerId, fragment, slideIn, slideOut, replace = true, addToBackstack = true)


inline fun <reified T : Any> Context.launchActivity(options: Bundle? = null, noinline init: Intent.() -> Unit = {}) {
    newIntent<T>(this).apply {
        init()
        startActivity(this, options)
    }
}

inline fun <reified T : Any> Activity.launchActivityForResult(
    requestCode: Int,
    options: Bundle? = null,
    noinline init: Intent.() -> Unit = {}
) {
    newIntent<T>(this).apply {
        init()
        startActivityForResult(this, requestCode, options)
    }
}

inline fun <reified T : Any> newIntent(context: Context): Intent =
    Intent(context, T::class.java)

fun AppCompatActivity.getCurrentFragment(): Fragment? =
    supportFragmentManager.fragments.firstOrNull { it.isVisible }

private fun AppCompatActivity.showFragment(
    containerId: Int,
    fragment: Fragment,
    slideIn: Int,
    slideOut: Int,
    replace: Boolean,
    addToBackstack: Boolean
) {
    supportFragmentManager.beginTransaction().apply {
        setCustomAnimations(slideIn, 0, 0, slideOut)
        if(replace) replace(containerId, fragment)
        else add(containerId, fragment)
        if(addToBackstack) addToBackStack(fragment.tag)
        commit()
    }
}