package minerva.android.main.base

import androidx.fragment.app.Fragment
import minerva.android.main.listener.FragmentInteractorListener

open class BaseFragment : Fragment() {
    lateinit var interactor: FragmentInteractorListener
    fun setListener(interactor: FragmentInteractorListener) {
        this.interactor = interactor
    }
}