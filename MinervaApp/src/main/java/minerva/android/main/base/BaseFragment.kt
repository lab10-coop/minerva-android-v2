package minerva.android.main.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.utils.DialogHandler
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable

open class BaseFragment : Fragment() {

    lateinit var interactor: FragmentInteractorListener
    fun setListener(interactor: FragmentInteractorListener) {
        this.interactor = interactor
    }

    fun handleAutomaticBackupError(
        it: Throwable,
        noAutomaticBackupErrorAction: () -> Unit = { showErrorToast() },
        positiveAction: () -> Unit = { }
    ) {
        if (it is AutomaticBackupFailedThrowable) {
            DialogHandler.showDialog(
                requireContext(),
                getString(R.string.error_header),
                getString(R.string.automatic_backup_failed_error)
            ) { positiveAction() }
        } else {
            noAutomaticBackupErrorAction()
        }
    }

    private fun showErrorToast() {
        Toast.makeText(requireContext(), getString(R.string.unexpected_error), Toast.LENGTH_LONG).show()
    }
}