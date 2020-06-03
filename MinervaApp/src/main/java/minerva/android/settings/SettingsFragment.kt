package minerva.android.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.launchActivity
import minerva.android.main.listener.BottomNavigationMenuListener
import minerva.android.settings.backup.BackupActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {

    private lateinit var listener: BottomNavigationMenuListener
    val viewModel: SettingsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onResume() {
        super.onResume()
        hideReminder()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backupItem.setOnClickListener { showBackupActivity() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as BottomNavigationMenuListener
    }

    private fun hideReminder() {
        if (viewModel.isMnemonicRemembered()) {
            listener.removeSettingsBadgeIcon()
            reminderView.gone()
            alertIcon.gone()
        }
    }

    private fun showBackupActivity() {
        context?.launchActivity<BackupActivity>()
    }
}
