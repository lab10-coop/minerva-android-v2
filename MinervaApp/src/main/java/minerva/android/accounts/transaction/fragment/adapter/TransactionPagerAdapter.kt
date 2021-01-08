package minerva.android.accounts.transaction.fragment.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TransactionPagerAdapter(activity: AppCompatActivity, private val getFragment: (Int) -> Fragment) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = NUM_PAGES
    override fun createFragment(position: Int): Fragment = getFragment(position)

    companion object {
        private const val NUM_PAGES = 2
    }
}