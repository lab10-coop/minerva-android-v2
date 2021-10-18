package minerva.android.services.dapps.model

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.services.dapps.dialog.OpenDappDialog

data class Dapp(
    val label: String = String.Empty,
    @DrawableRes val background: Int = Int.EmptyResource,
    val openDappDialogData: OpenDappDialog.Data = OpenDappDialog.Data()
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Dapp>() {
            override fun areItemsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem.label == newItem.label
            override fun areContentsTheSame(oldItem: Dapp, newItem: Dapp) = oldItem == newItem
        }
    }
}
