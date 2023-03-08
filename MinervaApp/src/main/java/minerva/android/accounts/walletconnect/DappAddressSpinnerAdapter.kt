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
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.utils.AddressConverter
import minerva.android.walletmanager.utils.AddressType

class DappAddressSpinnerAdapter(
    context: Context,
    @LayoutRes
    private val layoutResource: Int,
    private val addresses: List<AddressWrapper>
) : ArrayAdapter<AddressWrapper>(context, layoutResource, addresses) {

    var selectedItemWidth: Int? = null

    override fun getCount(): Int = addresses.size

    override fun getItem(position: Int): AddressWrapper = addresses[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, true)

    private fun createView(position: Int, parent: ViewGroup, isDropdown: Boolean): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            getItem(position).let { item ->
                SpinnerNetworkWalletConnectBinding.bind(this).network.apply {
                    text = "#${item.index.inc()}: ${AddressConverter.getShortAddress(AddressType.NORMAL_ADDRESS, item.address)}"
                    setTextColor(ContextCompat.getColor(context, R.color.gray))
                    val arrowRes = if (isDropdown) {
                        selectedItemWidth?.let { selectedItemWidth -> width = selectedItemWidth }
                        height = dpToPx(DROPDOWN_HEIGHT)
                        maxLines = Int.OneElement
                        Int.EmptyResource
                    } else {
                        R.drawable.ic_dropdown_purple
                    }
                    setCompoundDrawablesWithIntrinsicBounds(Int.EmptyResource, Int.EmptyResource, arrowRes, Int.EmptyResource)
                }
            }
        }

    companion object {
        private const val DROPDOWN_HEIGHT = 40f
    }
}