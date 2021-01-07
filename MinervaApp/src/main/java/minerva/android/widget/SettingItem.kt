package minerva.android.widget

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.setting_item_layout.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.settings.model.SettingRow

class SettingItem(context: Context) : ConstraintLayout(context) {

    init {
        inflate(context, R.layout.setting_item_layout, this)
    }

    fun setRow(settingRow: SettingRow) {
        settingRow.run {
            settingName.text = name
            detailMessage.text = detailText
            settingsArrow.visibleOrGone(isArrowVisible)
            mainNetworksSwitch.visibleOrGone(isSwitchVisible)
        }
    }

    fun setIcons(iconId: Int, rightIcon: Int = 0) {
        settingName.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, rightIcon, 0)
    }

    fun toggleSwitch(onCheckedChange: (isChecked: Boolean) -> Unit) {
        mainNetworksSwitch.setOnClickListener {
            onCheckedChange(mainNetworksSwitch.isChecked)
        }
    }

    fun setNetworkSwitch(isChecked: Boolean) {
        mainNetworksSwitch.isChecked = isChecked
    }
}