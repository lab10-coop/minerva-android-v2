package minerva.android.main.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable

open class BaseFragment : Fragment() {

    lateinit var interactor: FragmentInteractorListener
    fun setListener(interactor: FragmentInteractorListener) {
        this.interactor = interactor
    }

    fun handleAutomaticBackupError(it: Throwable) {
        val errorMessage: String = if (it is AutomaticBackupFailedThrowable) getString(R.string.automatic_backup_failed_error)
        else getString(R.string.unexpected_error)
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }
}