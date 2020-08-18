package minerva.android.identities.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import minerva.android.identities.IdentitiesFragment.Companion.CONTACTS_POSITION
import minerva.android.identities.IdentitiesFragment.Companion.MY_IDENTITIES_POSITION
import minerva.android.identities.contacts.ContactsFragment
import minerva.android.identities.credentials.CredentialsFragment
import minerva.android.identities.myIdentities.MyIdentitiesFragment

class IdentitiesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            MY_IDENTITIES_POSITION -> MyIdentitiesFragment.newInstance()
            CONTACTS_POSITION -> ContactsFragment.newInstance()
            else -> CredentialsFragment.newInstance()
        }
    }

    companion object {
        private const val ITEM_COUNT = 3
    }
}