package minerva.android.settings.model

import androidx.fragment.app.Fragment
import minerva.android.BuildConfig
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class Settings(
    val sectionTitle: String = String.Empty,
    val rows: List<SettingRow> = listOf(),
    val type: SettingsType
)

data class SettingRow(
    val name: String,
    val iconId: Int = Int.InvalidValue,
    val detailText: String = String.Empty,
    val isSwitchVisible: Boolean = false,
    val isArrowVisible: Boolean = true,
    val rowType: SettingsRowType
)

enum class SettingsType {
    SECURITY, PREFERENCES, INFO, LEGAL
}

enum class SettingsRowType {
    BACKUP, REMINDER_VIEW, AUTHENTICATION, EDIT_NETWORKS, MAIN_NETWORKS, CURRENCY, LANGUAGE, TWITTER, COMMUNITY, APP_VERSION, LICENCE
}

fun Fragment.propagateSettings(): List<Settings> =
    listOf(
        Settings(
            getString(R.string.security), listOf(
                SettingRow(getString(R.string.backup), R.drawable.ic_backup, rowType = SettingsRowType.BACKUP)
            ), SettingsType.SECURITY
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
            ), SettingsType.PREFERENCES
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
            ), SettingsType.INFO
        ),
        Settings(
            getString(R.string.legal), listOf(
                SettingRow(
                    getString(R.string.licence),
                    detailText = BuildConfig.LICENCE_TYPE,
                    isArrowVisible = false,
                    rowType = SettingsRowType.LICENCE
                )
            ), SettingsType.LEGAL
        )
    )
