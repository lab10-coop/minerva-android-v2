package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.databinding.SpinnerNetworkWalletConnectBinding
import minerva.android.extension.dpToPx
import minerva.android.extension.visibleOrInvisible

class DappNetworksSpinnerAdapter(
    context: Context,
    @LayoutRes
    private val layoutResource: Int,
    private val networks: List<NetworkDataSpinnerItem>
) : ArrayAdapter<NetworkDataSpinnerItem>(context, layoutResource, networks) {

    override fun getCount(): Int = networks.size

    override fun getItem(position: Int): NetworkDataSpinnerItem = networks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, true)

    private fun createView(position: Int, parent: ViewGroup, isDropdown: Boolean): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            networks[position].let { networkItem ->
                SpinnerNetworkWalletConnectBinding.bind(this).apply {
                    network.text = networkItem.networkName
                    arrow.visibleOrInvisible(!isDropdown)
                    val colorRes = if (isDropdown) {
                        layoutParams.height = dpToPx(32f)
                        R.color.darkGray80
                    } else {
                        R.color.warningOrange
                    }
                    network.setTextColor(ContextCompat.getColor(context, colorRes))
                }
            }
        }
}