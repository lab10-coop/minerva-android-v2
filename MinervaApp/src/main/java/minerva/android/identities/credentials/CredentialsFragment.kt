package minerva.android.identities.credentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import minerva.android.R

class CredentialsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_credentials, container, false)

    companion object {
        @JvmStatic
        fun newInstance() = CredentialsFragment()
    }
}