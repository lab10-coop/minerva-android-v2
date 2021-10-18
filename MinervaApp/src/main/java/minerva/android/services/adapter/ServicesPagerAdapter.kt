package minerva.android.services.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import minerva.android.services.ServicesFragment.Companion.CARD_NUMBER

class ServicesPagerAdapter(fragment: Fragment, private val fragments: (Int) -> Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = CARD_NUMBER
    override fun createFragment(position: Int): Fragment = fragments(position)
}