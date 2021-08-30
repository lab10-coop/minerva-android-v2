package minerva.android.accounts.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import minerva.android.R
import minerva.android.databinding.SpinnerNetworkBinding
import minerva.android.walletmanager.model.network.Network
import minerva.android.widget.repository.getNetworkIcon

class NetworkSpinnerAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val networks: List<Network>) :
    ArrayAdapter<Network>(context, layoutResource, networks) {

    override fun getCount(): Int = networks.size

    override fun getItem(position: Int): Network = networks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, true)

    private fun createView(position: Int, parent: ViewGroup, isDropDown: Boolean): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            networks[position].let { network ->
                SpinnerNetworkBinding.bind(this).row.apply {
                    text = network.name
                    val arrow = if (isDropDown) {
                        null
                    } else {
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_dropdown_black, null)
                    }
                    setCompoundDrawablesWithIntrinsicBounds(getNetworkIcon(context, network.chainId), null, arrow, null)
                }
            }
        }
}