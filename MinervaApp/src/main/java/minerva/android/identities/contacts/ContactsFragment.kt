package minerva.android.identities.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import minerva.android.R
import minerva.android.main.base.BaseFragment

class ContactsFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_contacts, container, false)

    companion object {
        @JvmStatic
        fun newInstance() = ContactsFragment()
    }
}