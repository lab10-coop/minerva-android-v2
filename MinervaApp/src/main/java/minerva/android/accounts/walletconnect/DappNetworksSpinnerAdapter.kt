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
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.kotlinUtils.OneElement

class DappNetworksSpinnerAdapter(
    context: Context,
    @LayoutRes
    private val layoutResource: Int,
    private val networks: List<NetworkDataSpinnerItem>
) : ArrayAdapter<NetworkDataSpinnerItem>(context, layoutResource, networks) {

    var selectedItemWidth: Int? = null

    override fun getCount(): Int = networks.size

    override fun getItem(position: Int): NetworkDataSpinnerItem = networks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, true)

    private fun createView(position: Int, parent: ViewGroup, isDropdown: Boolean): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            networks[position].let { networkItem ->
                SpinnerNetworkWalletConnectBinding.bind(this).network.apply {
                    text = networkItem.networkName
                    val networkRes = if (isDropdown) {
                        selectedItemWidth?.let { selectedItemWidth -> width = selectedItemWidth }
                        height = dpToPx(DROPDOWN_HEIGHT)
                        maxLines = Int.OneElement
                        R.color.darkGray80 to Int.EmptyResource
                    } else {
                        R.color.warningOrange to R.drawable.ic_dropdown_yellow
                    }
                    setTextColor(ContextCompat.getColor(context, networkRes.first))
                    setCompoundDrawablesWithIntrinsicBounds(
                        Int.EmptyResource,
                        Int.EmptyResource,
                        networkRes.second,
                        Int.EmptyResource
                    )
                }
            }
        }

    companion object {
        private const val DROPDOWN_HEIGHT = 40f
    }
}