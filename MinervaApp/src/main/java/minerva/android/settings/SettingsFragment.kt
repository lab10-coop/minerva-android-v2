package minerva.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.settings.backup.BackupActivity

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backupItem.setOnClickListener { showBackupActivity() }
    }

    private fun showBackupActivity() {
        context?.launchActivity<BackupActivity>()
    }
}
