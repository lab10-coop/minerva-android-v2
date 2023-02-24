package minerva.android.widget

import android.content.Context
import android.widget.LinearLayout
import minerva.android.R
import minerva.android.databinding.AdvancedActionItemLayoutBinding
import minerva.android.extension.visibleOrGone
import minerva.android.settings.advanced.model.AdvancedSection
import minerva.android.settings.advanced.model.AdvancedSectionRowType

/**
 * Advanced Action Item - class for showing AdvancedAction items
 * @param context - instance of android.content.Context
 */
class AdvancedActionItem(context: Context) : LinearLayout(context) {
    private val binding: AdvancedActionItemLayoutBinding =
        AdvancedActionItemLayoutBinding.bind(inflate(context, R.layout.advanced_action_item_layout, this))

    /**
     * Set Row - method which call methods for creating/filling row(layout) data
     * @param section - data for filling row details
     * @param callback - callback method for calling it while row pressed
     */
    fun setRow(section: AdvancedSection, callback: (type: AdvancedSectionRowType) -> Unit) {
        setDetails(section.rowType)
        when (section.rowType) {
            AdvancedSectionRowType.CHANGE_NETWORK_PROMPT -> setSwitchWidget(section.isSwitchChecked, callback)
            AdvancedSectionRowType.CLEAR_TOKEN_CACHE -> setMixedWidget(section)
        }
    }

    /**
     * Set Switch Widget - set details of switch(checkbox) widget
     * @param isChecked - state of switcher
     * @param onSettingPressed - callback for switcher (main listener of item (parent View) doesn't work for SwitchMaterial)
     */
    private fun setSwitchWidget(isChecked: Boolean, onSettingPressed: (type: AdvancedSectionRowType) -> Unit) =
        binding.apply {
            advancedActionChangeNetworkSwitch.apply {//set switcher details
                advancedActionChangeNetworkSwitch.isChecked = isChecked
                setOnClickListener {
                    onSettingPressed(AdvancedSectionRowType.CHANGE_NETWORK_PROMPT)
                }
            }
        }

    /**
     * Set Mixed Widget - set details of mixed widget
     * @param section - details of mixed widget
     */
    private fun setMixedWidget(section: AdvancedSection) =
        binding.apply {
            advancedActionText.apply {//set mixed widget details
                text = resources.getString(section.actionText)
            }
        }

    /**
     * Set Details - method for show/hide layout details
     * @param type - specified type of row (layout)
     */
    private fun setDetails(type: AdvancedSectionRowType) =
        binding.apply {
            if (AdvancedSectionRowType.CHANGE_NETWORK_PROMPT == type) {
                advancedActionText.visibleOrGone(false)
                advancedActionArrow.visibleOrGone(false)
                advancedActionChangeNetworkSwitch.visibleOrGone(true)
            } else if (AdvancedSectionRowType.CLEAR_TOKEN_CACHE == type) {
                advancedActionText.visibleOrGone(true)
                advancedActionArrow.visibleOrGone(true)
                advancedActionChangeNetworkSwitch.visibleOrGone(false)
            }
        }
}