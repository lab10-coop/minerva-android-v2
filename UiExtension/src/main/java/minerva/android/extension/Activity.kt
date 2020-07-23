package minerva.android.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


fun AppCompatActivity.addFragment(containerId: Int, fragment: Fragment, slideIn: Int = 0, slideOut: Int = 0) {
    supportFragmentManager.beginTransaction().apply {
        setCustomAnimations(slideIn, 0, 0, slideOut)
        add(containerId, fragment)
        commit()
    }
}

fun AppCompatActivity.replaceFragment(containerId: Int, fragment: Fragment, slideIn: Int = 0, slideOut: Int = 0) {
    supportFragmentManager.beginTransaction().apply {
        setCustomAnimations(slideIn, 0, 0, slideOut)
        replace(containerId, fragment)
        addToBackStack(null)
        commit()
    }
}

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