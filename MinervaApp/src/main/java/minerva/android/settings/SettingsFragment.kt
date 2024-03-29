package minerva.android.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.databinding.FragmentSettingsBinding
import minerva.android.extension.launchActivity
import minerva.android.extension.openUri
import minerva.android.extensions.showBiometricPrompt
import minerva.android.main.base.BaseFragment
import minerva.android.settings.adapter.SettingsAdapter
import minerva.android.settings.backup.BackupActivity
import minerva.android.settings.model.SettingsRowType
import minerva.android.settings.model.SettingsRowType.*
import minerva.android.settings.model.propagateSettings
import minerva.android.wrapped.startAdvancedWrappedActivity
import minerva.android.wrapped.startAppVersionWrappedActivity
import minerva.android.wrapped.startAuthenticationWrappedActivity
import minerva.android.wrapped.startCurrencyWrappedActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding
    val viewModel: SettingsViewModel by viewModel()

    private val settingsAdapter by lazy {
        SettingsAdapter({ onSettingsRowClicked(it) }, { onUseMainNetworkCheckedChange(it) })
    }

    private val connectivityManager by lazy {
        requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    }

    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                requireActivity().runOnUiThread { updateSettingsAdapter() }
            }

            override fun onLost(network: Network) {
                requireActivity().runOnUiThread { updateSettingsAdapter() }
                super.onLost(network)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)
        setupRecycleView()
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.white)
        hideReminder()
        setupConnectionCallbacks()
    }

    override fun onPause() {
        super.onPause()
        clearConnectionCallbacks()
    }

    private fun setupRecycleView() {
        binding.settingsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = settingsAdapter
        }
    }

    private fun hideReminder() {
        with(viewModel) {
            if (isMnemonicRemembered && isSynced) {
                interactor.removeSettingsBadgeIcon()
            }
            updateSettingsAdapter()
        }
    }

    private fun updateSettingsAdapter() {
        with(viewModel) {
            mapOf(
                MNEMONIC_REMEMBERED to isMnemonicRemembered,
                MAIN_NETWORKS_ENABLED to areMainNetsEnabled,
                AUTHENTICATION_ENABLED to isAuthenticationEnabled
            ).let { flagsMap ->
                settingsAdapter.updateList(
                    flagsMap,
                    propagateSettings(viewModel.getCurrentFiat(requireContext().resources.getStringArray(R.array.currencies)))
                )
            }
        }
    }

    private fun showBackupActivity() =
        if (viewModel.isAuthenticationEnabled) showBiometricPrompt({ startBackupActivity() })
        else startBackupActivity()

    private fun startBackupActivity() = context?.launchActivity<BackupActivity>()

    private fun onSettingsRowClicked(type: SettingsRowType) {
        when (type) {
            BACKUP -> showBackupActivity()
            TWITTER -> context?.openUri(BuildConfig.TWITTER_APP, BuildConfig.TWITTER_WEB)
            VISIT_MINERVA -> context?.openUri(webUri = BuildConfig.VISIT_MINERVA)
            OFFICIAL_MINERVA_LINK3 -> context?.openUri(webUri = BuildConfig.OFFICIAL_MINERVA_LINK3)
            TERMS_OF_SERVICE -> context?.openUri(webUri = BuildConfig.TERMS_OF_SERVICE)
            PRIVACY_POLICY -> context?.openUri(webUri = BuildConfig.PRIVACY_POLICY)
            AUTHENTICATION -> startAuthenticationWrappedActivity(requireContext())
            CURRENCY -> startCurrencyWrappedActivity(requireContext())
            APP_VERSION -> startAppVersionWrappedActivity(requireContext())
            ADVANCED -> startAdvancedWrappedActivity(requireContext())
            else -> Timber.d(type.toString())
        }
    }

    private fun onUseMainNetworkCheckedChange(isChecked: Boolean) {
        viewModel.areMainNetworksEnabled(isChecked)
    }

    private fun setupConnectionCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        }
    }


    private fun clearConnectionCallbacks() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    fun isNetworkConnected(): Boolean {
        return connectivityManager?.activeNetwork != null
    }

    companion object {
        const val MNEMONIC_REMEMBERED = 0
        const val MAIN_NETWORKS_ENABLED = 1
        const val AUTHENTICATION_ENABLED = 2
    }
}
