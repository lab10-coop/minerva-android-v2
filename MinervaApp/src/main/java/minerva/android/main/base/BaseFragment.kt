package minerva.android.main.base

import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.hitanshudhawan.spannablestringparser.spannify
import minerva.android.R
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable

open class BaseFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

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
            AlertDialogHandler.showDialog(
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

    fun getHeader(areMainNetworkEnabled: Boolean) =
        if (areMainNetworkEnabled) {
            "${getText(R.string.main_networks)} {` ${getString(R.string.beta_funds_at_risk)}` < text-color:#DD2B00 />}".spannify()
        } else {
            getString(R.string.test_networks)
        }
}