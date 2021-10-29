package minerva.android.services.dapps.model

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.services.dapps.dialog.OpenDappDialog

data class Dapp(
    val shortName: String = String.Empty,
    val longName: String = String.Empty,
    val description: String = String.Empty,
    val colorHex: String = String.Empty,
    val iconUrl: String = String.Empty,
    @DrawableRes val iconDrawable: Int = Int.EmptyResource,
    val openDappDialogData: OpenDappDialog.Data = OpenDappDialog.Data()
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Dapp>() {
            override fun areItemsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem.shortName == newItem.shortName
            override fun areContentsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem == newItem
        }
    }
}
