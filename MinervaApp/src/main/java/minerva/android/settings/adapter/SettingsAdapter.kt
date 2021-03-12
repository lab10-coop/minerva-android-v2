package minerva.android.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.settings_section_layout.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.settings.model.SettingRow
import minerva.android.settings.model.Settings
import minerva.android.settings.model.SettingsRowType
import minerva.android.settings.model.SettingsSection
import minerva.android.widget.ReminderView
import minerva.android.widget.SettingItem

class SettingsAdapter(
    private val onSettingPressed: (type: SettingsRowType) -> Unit,
    private val onCheckedChange: (isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<SettingsViewHolder>() {

    var settings: List<Settings> = listOf()
    private var isMnemonicRemembered: Boolean = false
    private var areMainNetworksEnabled: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder =
        SettingsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_section_layout, parent, false))

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bindData(
            settings[position],
            Pair(isMnemonicRemembered, areMainNetworksEnabled),
            { onSettingPressed(it) }) { onCheckedChange(it) }
    }

    override fun getItemCount(): Int = settings.size

    fun updateList(flags: Pair<Boolean, Boolean>, settings: List<Settings>) {
        val (isMnemonicRemembered, areMainNetsEnabled) = flags
        this.settings = settings
        this.isMnemonicRemembered = isMnemonicRemembered
        this.areMainNetworksEnabled = areMainNetsEnabled
        notifyDataSetChanged()
    }
}

class SettingsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bindData(
        settings: Settings,
        flags: Pair<Boolean, Boolean>,
        onSettingPressed: (type: SettingsRowType) -> Unit,
        onCheckedChange: (isChecked: Boolean) -> Unit
    ) {
        view.run {
            sectionTitle.text = settings.sectionTitle
            addSettingRows(settings, flags, onSettingPressed, onCheckedChange)
            settingsSeparator.visibleOrGone(settings.section != SettingsSection.LEGAL)
        }
    }

    private fun View.addSettingRows(
        settings: Settings,
        flags: Pair<Boolean, Boolean>,
        onSettingPressed: (type: SettingsRowType) -> Unit,
        onCheckedChange: (isChecked: Boolean) -> Unit
    ) {
        val (isMnemonicRemembered, areMainNetsEnabled) = flags
        settingRows.removeAllViews()

        if (shouldShowAlerts(settings)) {
            settingRows.addView(
                ReminderView(
                    context,
                    rows = settings.rows.filter { it.rowType == SettingsRowType.REMINDER_VIEW && it.isVisible })
            )
        }

        settings.rows.filter { it.rowType != SettingsRowType.REMINDER_VIEW }.forEach { settingRow ->
            settingRows.addView(SettingItem(context).apply {
                setRow(settingRow)
                setOnClickListener {
                    if (settingRow.isSwitchVisible) toggleSwitch { onCheckedChange(it) }
                    else onSettingPressed(settingRow.rowType)
                }
                setAlert(settingRow, isMnemonicRemembered, false) //TODO klop add saving authentication enable option
                if (settingRow.isSwitchVisible) {
                    setNetworkSwitch(areMainNetsEnabled)
                }
            })
        }
    }

    private fun shouldShowAlerts(settings: Settings) =
        settings.section == SettingsSection.SECURITY && settings.rows.any { it.rowType == SettingsRowType.REMINDER_VIEW && it.isVisible }

    private fun SettingItem.setAlert(settingRow: SettingRow, isMnemonicRemembered: Boolean, isAuthenticationEnabled: Boolean) {
        if ((settingRow.rowType == SettingsRowType.BACKUP && !isMnemonicRemembered)
            || (settingRow.rowType == SettingsRowType.AUTHENTICATION && !isAuthenticationEnabled)
        ) showAlert()
    }
}