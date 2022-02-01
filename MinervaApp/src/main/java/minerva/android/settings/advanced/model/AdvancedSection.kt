package minerva.android.settings.advanced.model

import minerva.android.R
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.settings.advanced.AdvancedFragment

data class AdvancedSection(
    val title: Int = Int.EmptyResource,
    val description: Int = Int.EmptyResource,
    val actionText: Int = Int.EmptyResource,
    val rowType: AdvancedSectionRowType
)

enum class AdvancedSectionRowType{
    CLEAR_TOKEN_CACHE
}

fun AdvancedFragment.propagateSections() = listOf<AdvancedSection>(
    AdvancedSection(
        R.string.clear_token_cache_title,
        R.string.clear_token_cache_description,
        R.string.clear_token_cache_action_text,
        AdvancedSectionRowType.CLEAR_TOKEN_CACHE
    )
)



