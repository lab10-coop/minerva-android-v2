package minerva.android.widget

import android.content.Context
import android.util.TypedValue
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.databinding.SettingItemLayoutBinding
import minerva.android.extension.addRippleEffect
import minerva.android.extension.empty
import minerva.android.extension.visibleOrGone
import minerva.android.settings.model.SettingRow

class SettingItem(context: Context) : ConstraintLayout(context) {

    private val binding: SettingItemLayoutBinding =
        SettingItemLayoutBinding.bind(inflate(context, R.layout.setting_item_layout, this))

    init {
        addRippleEffect()
    }

    fun setRow(settingRow: SettingRow) {
        binding.apply {
            settingRow.run {
                settingName.text = name
                detailMessage.text = detailText
                settingsArrow.visibleOrGone(isArrowVisible)
                mainNetworksSwitch.visibleOrGone(isSwitchVisible)
            }
        }
    }

    fun showAlert(isAlerted: Boolean) {
        binding.apply {
            if (isAlerted) {
                detailMessage.setTextColor(ContextCompat.getColor(context, R.color.alertRed))
                settingsArrow.setColorFilter(ContextCompat.getColor(context, R.color.alertRed))
            } else detailMessage.text = String.empty
        }

    }

    fun toggleSwitch(onCheckedChange: (isChecked: Boolean) -> Unit) {
        binding.apply {
            mainNetworksSwitch.toggle()
            onCheckedChange(mainNetworksSwitch.isChecked)
        }
    }

    fun setNetworkSwitch(isChecked: Boolean) {
        binding.apply {
            mainNetworksSwitch.isChecked = isChecked
        }
    }
}