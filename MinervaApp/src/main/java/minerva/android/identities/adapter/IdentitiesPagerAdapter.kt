package minerva.android.identities.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import minerva.android.identities.myIdentities.MyIdentitiesFragment

class IdentitiesPagerAdapter(fragment: Fragment, private val fragments: (Int) -> Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = LAST_ELEMENT
    override fun createFragment(position: Int): Fragment = fragments(position)

    companion object {
        private const val LAST_ELEMENT = 300
    }
}