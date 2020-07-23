package minerva.android.accounts.transaction.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Recipient


class RecipientAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val recipients: List<Recipient>) :
    ArrayAdapter<Recipient>(context, layoutResource, recipients), Filterable {

    private var currentRecipient = recipients

    override fun getCount(): Int = currentRecipient.size

    override fun getItem(position: Int): Recipient? = currentRecipient[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createViewFromResource(position, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View = createViewFromResource(position, parent)

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                currentRecipient = filterResults.values as List<Recipient>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val queryString = charSequence?.toString()
                val filterResults = FilterResults()
                filterResults.values = if (queryString == null || queryString.isEmpty()) {
                    recipients
                } else {
                    recipients.filter { containsQueryInEnsOrAddress(queryString, it) }
                }
                return filterResults
            }
        }
    }

    private fun createViewFromResource(position: Int, parent: ViewGroup?): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            findViewById<TextView>(R.id.ensName).let { ensName ->
                findViewById<TextView>(R.id.recipientAddress).let { address ->
                    currentRecipient[position].ensName.let {
                        ensName.text = it
                        address.text = recipients[position].address
                        ensName.visibleOrGone(it != String.Empty)
                        address.visibleOrGone(it == String.Empty)
                    }
                }
            }
        }

    private fun containsQueryInEnsOrAddress(queryString: String, recipient: Recipient) =
        recipient.ensName.contains(queryString, true) || recipient.address.contains(queryString, true)
}