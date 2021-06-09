package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.AddressListRowBinding

class AddressesAdapter : RecyclerView.Adapter<AddressViewHolder>() {

    private var selectedPosition: Int = FIRST_POSITION

    private var addresses: List<Pair<Int, String>> = emptyList()

    fun isEmpty() = addresses.isEmpty()

    fun updateList(newAddresses: List<Pair<Int, String>>) {
        addresses = newAddresses
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = addresses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder =
        AddressViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.address_list_row, parent, false))


    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {

        holder.bind(addresses[position], selectedPosition == position) {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedIndex() = addresses[selectedPosition].first

    companion object {
        private const val FIRST_POSITION = 0
    }
}

class AddressViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val binding = AddressListRowBinding.bind(view)

    fun bind(address: Pair<Int, String>, isChecked: Boolean, onClick: () -> Unit) = with(binding) {
        checkButton.isEnabled = isChecked
        setAddressData(address)
        view.setOnClickListener { onClick() }
    }

    private fun setAddressData(addressItem: Pair<Int, String>) = with(binding) {
        address.text = addressItem.second
        indexValue.text = String.format(ACCOUNT_INDEX_PATTERN, addressItem.first.inc())
    }

    companion object {
        private const val ACCOUNT_INDEX_PATTERN = "#%d"
    }
}

