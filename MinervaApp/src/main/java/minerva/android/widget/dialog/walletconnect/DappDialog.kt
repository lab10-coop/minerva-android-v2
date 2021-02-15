package minerva.android.widget.dialog.walletconnect

import android.content.Context
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding

abstract class DappDialog(context: Context) : BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    abstract val networkHeader: DappNetworkHeaderBinding

    init {
        this.setCancelable(false)
    }

    fun setupHeader(dapppName: String, networkName: String, icon: Any) = with(networkHeader) {
        name.text = dapppName
        network.text = networkName
        Glide.with(context)
            .load(icon)
            .into(networkHeader.icon)
    }
}