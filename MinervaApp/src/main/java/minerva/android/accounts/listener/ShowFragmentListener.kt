package minerva.android.accounts.listener

import androidx.fragment.app.Fragment

interface ShowFragmentListener {
    fun showFragment(fragment: Fragment, slideIn: Int, slideOut: Int, title: String? = null)
    fun setActionBarTitle(title: String?)
}