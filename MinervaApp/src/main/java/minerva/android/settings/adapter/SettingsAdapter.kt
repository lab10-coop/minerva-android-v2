package minerva.android.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.settings_section_layout.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.settings.model.SettingRow
import minerva.android.settings.model.Settings
import minerva.android.settings.model.SettingsRowType
import minerva.android.settings.model.SettingsType
import minerva.android.widget.BackupReminderView
import minerva.android.widget.SettingItem

class SettingsAdapter(
    private val onSettingPressed: (type: SettingsRowType) -> Unit,
    private val onCheckedChange: (isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<SettingsViewHolder>() {

    var settings: List<Settings> = listOf()
    private var isMnemonicRemembered: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder =
        SettingsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_section_layout, parent, false))

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bindData(settings[position], isMnemonicRemembered, { onSettingPressed(it) }, { onCheckedChange(it) })
    }

    override fun getItemCount(): Int = settings.size

    fun updateList(isMnemonicRemembered: Boolean, settings: List<Settings>) {
        this.settings = settings
        this.isMnemonicRemembered = isMnemonicRemembered
        notifyDataSetChanged()
    }
}

class SettingsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bindData(
        settings: Settings,
        isMnemonicRemembered: Boolean,
        onSettingPressed: (type: SettingsRowType) -> Unit,
        onCheckedChange: (isChecked: Boolean) -> Unit
    ) {
        view.run {
            sectionTitle.text = settings.sectionTitle
            addSettingRows(settings, isMnemonicRemembered, onSettingPressed, onCheckedChange)
            settingsSeparator.visibleOrGone(settings.type != SettingsType.LEGAL)
        }
    }

    private fun View.addSettingRows(
        settings: Settings,
        isMnemonicRemembered: Boolean,
        onSettingPressed: (type: SettingsRowType) -> Unit,
        onCheckedChange: (isChecked: Boolean) -> Unit
    ) {
        settingRows.removeAllViews()

        if (settings.type == SettingsType.SECURITY && !isMnemonicRemembered) {
            settingRows.addView(BackupReminderView(context))
        }

        settings.rows.forEach { settingRow ->
            settingRows.addView(SettingItem(context).apply {
                setRow(settingRow)
                setOnClickListener { onSettingPressed(settingRow.rowType) }
                setIcon(settingRow, isMnemonicRemembered)
                toggleSwitch { onCheckedChange(it) }
            })
        }
    }

    private fun SettingItem.setIcon(settingRow: SettingRow, isMnemonicRemembered: Boolean) {
        if (settingRow.iconId != Int.InvalidValue) {
            showIcons(settingRow, isMnemonicRemembered)
        }
    }

    private fun SettingItem.showIcons(settingRow: SettingRow, isMnemonicRemembered: Boolean) {
        if (settingRow.rowType == SettingsRowType.BACKUP && !isMnemonicRemembered) setIcons(settingRow.iconId, R.drawable.ic_alert)
        else setIcons(settingRow.iconId)
    }
}