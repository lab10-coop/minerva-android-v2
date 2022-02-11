package minerva.android.services.dapps.model

import androidx.recyclerview.widget.DiffUtil
import minerva.android.extension.empty
import minerva.android.kotlinUtils.Empty

data class Dapp(
    val shortName: String = String.Empty,
    val longName: String = String.Empty,
    val description: String = String.Empty,
    val colorHex: String = String.Empty,
    val iconUrl: String = String.Empty,
    val dappUrl: String = String.empty,
    val isSponsored: Boolean = false,
    val sponsoredOrder: Int = NOT_SPONSORED_ORDER_ID,
    var isFavorite: Boolean = false
) {
    companion object {
        private const val NOT_SPONSORED_ORDER_ID = 0
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Dapp>() {
            override fun areItemsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem.shortName == newItem.shortName
            override fun areContentsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem == newItem
        }
    }
}
