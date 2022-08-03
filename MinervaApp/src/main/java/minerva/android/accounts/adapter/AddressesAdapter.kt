package minerva.android.accounts.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.AddressListRowBinding
import minerva.android.walletmanager.model.AddressStatus
import minerva.android.walletmanager.model.AddressWrapper

class AddressesAdapter(private val onAddressClick: (Boolean) -> Unit) : RecyclerView.Adapter<AddressViewHolder>() {

    private var selectedPosition: Int = WRONG_POSITION

    private var addresses: List<AddressWrapper> = emptyList()

    fun isEmptyOrNoSelection() = addresses.isEmpty() || selectedPosition == WRONG_POSITION

    fun updateList(newAddresses: List<AddressWrapper>) {
        addresses = newAddresses
        selectedPosition = WRONG_POSITION
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = addresses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder =
        AddressViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.address_list_row, parent, false), parent.context)


    override fun onBindViewHolder(holder: AddressViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val addressWrapper: AddressWrapper = addresses[position]
        holder.bind(addressWrapper, selectedPosition == position) {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            //prevent action if address state doesn't allow work with it
            if (addressWrapper.status != AddressStatus.ALREADY_IN_USE) {
                onAddressClick(isEmptyOrNoSelection())
            }
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedIndex() = addresses.getOrNull(selectedPosition)?.index

    companion object {
        private const val WRONG_POSITION = -1
    }
}

class AddressViewHolder(private val view: View, private val context: Context) : RecyclerView.ViewHolder(view) {

    private val binding = AddressListRowBinding.bind(view)

    fun bind(address: AddressWrapper, isChecked: Boolean, onClick: () -> Unit) = with(binding) {
        checkButton.isEnabled = isChecked
        setAddressData(address)
        view.setOnClickListener { onClick() }
    }

    private fun setAddressData(addressItem: AddressWrapper) = with(binding) {
        address.text = addressItem.address
        indexValue.text = String.format(ACCOUNT_INDEX_PATTERN, addressItem.index.inc())
        when (addressItem.status) {
            AddressStatus.ALREADY_IN_USE -> {
                addressStatus.text = context.getString(R.string.already_in_use)
                addressStatus.setTextColor(context.getColor(R.color.dividerGray))
                checkButton.visibility = View.GONE
            }
            AddressStatus.FREE -> {
                addressStatus.text = context.getString(R.string.free)
                addressStatus.setTextColor(context.getColor(R.color.saturatedGreen))
                checkButton.visibility = View.VISIBLE
            }
            AddressStatus.HIDDEN -> {
                addressStatus.text = context.getString(R.string.hidden)
                addressStatus.setTextColor(context.getColor(R.color.lightGreen))
                checkButton.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val ACCOUNT_INDEX_PATTERN = "#%d"
    }
}

