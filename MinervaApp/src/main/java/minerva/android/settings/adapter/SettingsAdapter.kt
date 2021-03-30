package minerva.android.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.settings_section_layout.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.settings.SettingsFragment
import minerva.android.settings.SettingsFragment.Companion.AUTHENTICATION_ENABLED
import minerva.android.settings.SettingsFragment.Companion.MAIN_NETWORKS_ENABLED
import minerva.android.settings.SettingsFragment.Companion.MNEMONIC_REMEMBERED
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

    private var flags = mapOf<Int, Boolean>()
    var settings: List<Settings> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder =
        SettingsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_section_layout, parent, false))

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bindData(
            settings[position],
            flags,
            { onSettingPressed(it) }) { onCheckedChange(it) }
    }

    override fun getItemCount(): Int = settings.size

    fun updateList(flags: Map<Int, Boolean>, settings: List<Settings>) {
        this.settings = settings
        this.flags = flags
        notifyDataSetChanged()
    }
}

class SettingsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bindData(
        settings: Settings,
        flags: Map<Int, Boolean>,
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
        flags: Map<Int, Boolean>,
        onSettingPressed: (type: SettingsRowType) -> Unit,
        onCheckedChange: (isChecked: Boolean) -> Unit
    ) {
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
                setAlert(settingRow, flags)
                if (settingRow.isSwitchVisible) {
                    setNetworkSwitch(flags[MAIN_NETWORKS_ENABLED] ?: false)
                }
            })
        }
    }

    private fun shouldShowAlerts(settings: Settings) =
        settings.section == SettingsSection.SECURITY && settings.rows.any { it.rowType == SettingsRowType.REMINDER_VIEW && it.isVisible }

    private fun SettingItem.setAlert(settingRow: SettingRow, flags: Map<Int, Boolean>) {
        when (settingRow.rowType) {
            SettingsRowType.BACKUP -> showAlert(!(flags[MNEMONIC_REMEMBERED] ?: false))
            SettingsRowType.AUTHENTICATION -> showAlert(!(flags[AUTHENTICATION_ENABLED] ?: false))
        }
    }
}