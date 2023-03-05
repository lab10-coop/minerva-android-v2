package minerva.android.settings.advanced.model

import minerva.android.kotlinUtils.EmptyResource

data class AdvancedSection(
    val title: Int = Int.EmptyResource,
    val description: Int = Int.EmptyResource,
    val actionText: Int = Int.EmptyResource,
    val rowType: AdvancedSectionRowType,
    val isSwitchChecked: Boolean = false
)

enum class AdvancedSectionRowType{
    CLEAR_TOKEN_CACHE,
    CHANGE_NETWORK_PROMPT
}