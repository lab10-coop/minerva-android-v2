package minerva.android.settings.model

import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.settings.SettingsFragment

data class Settings(
    val sectionTitle: String = String.Empty,
    val rows: List<SettingRow> = listOf(),
    val section: SettingsSection,
    val shouldDisplaySeparator: Boolean = true
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

enum class SettingsRowType(val iconRes: Int) {
    BACKUP(R.drawable.ic_backup),
    REMINDER_VIEW(Int.EmptyResource),
    AUTHENTICATION(R.drawable.ic_authentication),
    EDIT_NETWORKS(Int.EmptyResource),
    MAIN_NETWORKS(R.drawable.ic_main_networks),
    CURRENCY(R.drawable.ic_currency),
    LANGUAGE(Int.EmptyResource),
    TWITTER(R.drawable.ic_twitter),
    VISIT_MINERVA(R.drawable.ic_visit_minerva),
    OFFICIAL_MINERVA_LINK3(R.drawable.ic_official_minerva_link3),
    APP_VERSION(Int.EmptyResource),
    LICENCE(Int.EmptyResource),
    TERMS_OF_SERVICE(Int.EmptyResource),
    PRIVACY_POLICY(Int.EmptyResource),
    ADVANCED(R.drawable.ic_setting_row)
}

fun SettingsFragment.propagateSettings(currentFiat: String): List<Settings> =
    listOf(
        Settings(
            getString(R.string.security), listOf(
                SettingRow(
                    getString(R.string.sync_alert_title),
                    detailText = getString(R.string.sync_alert_message),
                    rowType = SettingsRowType.REMINDER_VIEW,
                    isVisible = !isNetworkConnected()
                ),
                SettingRow(
                    getString(R.string.backup_alert_title),
                    detailText = getString(R.string.backup_alert_message),
                    rowType = SettingsRowType.REMINDER_VIEW,
                    isVisible = !viewModel.isMnemonicRemembered
                ),
                SettingRow(
                    getString(R.string.authentication_alert_title),
                    detailText = getString(R.string.authentication_alert_message),
                    rowType = SettingsRowType.REMINDER_VIEW,
                    isVisible = !viewModel.isAuthenticationEnabled
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
                    getString(R.string.currency),
                    R.drawable.ic_currency,
                    detailText = currentFiat,
                    rowType = SettingsRowType.CURRENCY
                ),
                SettingRow(
                    getString(R.string.use_main_networks),
                    R.drawable.ic_main_networks,
                    isSwitchVisible = true,
                    isArrowVisible = false,
                    rowType = SettingsRowType.MAIN_NETWORKS
                ),
                SettingRow(
                    getString(R.string.advanced),
                    R.drawable.ic_setting_row,
                    isSwitchVisible = false,
                    isArrowVisible = true,
                    rowType = SettingsRowType.ADVANCED
                )
            ), SettingsSection.PREFERENCES
        ),
        Settings(
            getString(R.string.info), listOf(
                SettingRow(getString(R.string.visit_minerva), R.drawable.ic_visit_minerva, rowType = SettingsRowType.VISIT_MINERVA),
                SettingRow(getString(R.string.follow_on_twitter), R.drawable.ic_twitter, rowType = SettingsRowType.TWITTER),
                SettingRow(getString(R.string.official_minerva_link3), R.drawable.ic_official_minerva_link3, rowType = SettingsRowType.OFFICIAL_MINERVA_LINK3),
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

            ), SettingsSection.LEGAL, false
        )
    )