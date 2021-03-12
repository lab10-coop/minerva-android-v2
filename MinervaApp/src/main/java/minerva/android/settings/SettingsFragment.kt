package minerva.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_settings.*
import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.extension.openUri
import minerva.android.main.base.BaseFragment
import minerva.android.settings.adapter.SettingsAdapter
import minerva.android.settings.backup.BackupActivity
import minerva.android.settings.model.SettingsRowType
import minerva.android.settings.model.SettingsRowType.*
import minerva.android.settings.model.propagateSettings
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    val viewModel: SettingsViewModel by viewModel()

    private val settingsAdapter by lazy {
        SettingsAdapter({ onSettingsRowClicked(it) }, { onUseMainNetworkCheckedChange(it) })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.white)
        hideReminder()
    }

    private fun setupRecycleView() {
        settingsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = settingsAdapter
        }
    }

    private fun hideReminder() {
        with(viewModel) {
            if (isMnemonicRemembered && isSynced) {
                interactor.removeSettingsBadgeIcon()
            }
            settingsAdapter.updateList(Pair(isMnemonicRemembered, areMainNetsEnabled), propagateSettings())
        }
    }

    private fun showBackupActivity() {
        context?.launchActivity<BackupActivity>()
    }

    private fun onSettingsRowClicked(type: SettingsRowType) {
        when (type) {
            BACKUP -> showBackupActivity()
            TWITTER -> context?.openUri(BuildConfig.TWITTER_APP, BuildConfig.TWITTER_WEB)
            COMMUNITY -> context?.openUri(BuildConfig.TELEGRAM_APP, BuildConfig.TELEGRAM_WEB)
            TERMS_OF_SERVICE -> context?.openUri(webUri = BuildConfig.TERMS_OF_SERVICE)
            PRIVACY_POLICY -> context?.openUri(webUri = BuildConfig.PRIVACY_POLICY)
            AUTHENTICATION -> Toast.makeText(requireContext(), "Open Authentication Settings", Toast.LENGTH_SHORT).show()
            else -> Timber.d(type.toString())
        }
    }

    private fun onUseMainNetworkCheckedChange(isChecked: Boolean) {
        viewModel.areMainNetworksEnabled(isChecked)
    }
}
