package minerva.android.settings.model

import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.settings.SettingsFragment

data class Settings(
    val sectionTitle: String = String.Empty,
    val rows: List<SettingRow> = listOf(),
    val section: SettingsSection
)

data class SettingRow(
    val name: String,
    val iconId: Int = Int.InvalidValue,
    val detailText: String = String.Empty,
    val isSwitchVisible: Boolean = false,
    val isArrowVisible: Boolean = true,
    val rowType: SettingsRowType,
    var isVisible: Boolean = true
)

enum class SettingsSection {
    SECURITY, PREFERENCES, INFO, LEGAL
}

enum class SettingsRowType {
    BACKUP, REMINDER_VIEW, AUTHENTICATION, EDIT_NETWORKS, MAIN_NETWORKS, CURRENCY, LANGUAGE, TWITTER, COMMUNITY, APP_VERSION, LICENCE, TERMS_OF_SERVICE, PRIVACY_POLICY
}

fun SettingsFragment.propagateSettings(): List<Settings> =
    listOf(
        Settings(
            getString(R.string.security), listOf(
                SettingRow(
                    getString(R.string.sync_alert_title),
                    detailText = getString(R.string.sync_alert_message),
                    rowType = SettingsRowType.REMINDER_VIEW,
                    isVisible = !viewModel.isSynced
                ),
                SettingRow(
                    getString(R.string.backup_alert_title),
                    detailText = getString(R.string.backup_alert_message),
                    rowType = SettingsRowType.REMINDER_VIEW,
                    isVisible = !viewModel.isMnemonicRemembered
                ),
                SettingRow(
                    getString(R.string.backup),
                    R.drawable.ic_backup,
                    detailText = getString(R.string.create),
                    rowType = SettingsRowType.BACKUP
                ),
                SettingRow(
                    getString(R.string.authentication),
                    R.drawable.ic_authentication,
                    detailText = getString(R.string.enable),
                    rowType = SettingsRowType.AUTHENTICATION
                )
            ), SettingsSection.SECURITY
        ),
        Settings(
            getString(R.string.your_preferences), listOf(
                SettingRow(
                    getString(R.string.use_main_networks),
                    R.drawable.ic_main_networks,
                    isSwitchVisible = true,
                    isArrowVisible = false,
                    rowType = SettingsRowType.MAIN_NETWORKS
                )
            ), SettingsSection.PREFERENCES
        ),
        Settings(
            getString(R.string.info), listOf(
                SettingRow(getString(R.string.follow_on_twitter), R.drawable.ic_twitter, rowType = SettingsRowType.TWITTER),
                SettingRow(getString(R.string.join_community), R.drawable.ic_community, rowType = SettingsRowType.COMMUNITY),
                SettingRow(
                    getString(R.string.version),
                    detailText = BuildConfig.VERSION_NAME,
                    rowType = SettingsRowType.APP_VERSION
                )
            ), SettingsSection.INFO
        ),
        Settings(
            getString(R.string.legal), listOf(
                SettingRow(
                    getString(R.string.terms_of_service),
                    isArrowVisible = true,
                    rowType = SettingsRowType.TERMS_OF_SERVICE
                ),
                SettingRow(
                    getString(R.string.privacy_policy),
                    isArrowVisible = true,
                    rowType = SettingsRowType.PRIVACY_POLICY
                )

            ), SettingsSection.LEGAL
        )
    )
