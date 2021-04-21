package minerva.android.settings.fiat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.FiatListRowBinding
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.mapper.StringArrayMapper

class FiatAdapter(
    private val fiats: List<String>,
    private var currentCheckedPosition: Int,
    private val tapAction: (position: Int) -> Unit
) :
    RecyclerView.Adapter<FiatViewHolder>() {

    override fun getItemCount(): Int = fiats.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FiatViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.fiat_list_row, parent, false)
    )

    override fun onBindViewHolder(holder: FiatViewHolder, position: Int) {
        holder.setData(fiats[position], currentCheckedPosition) { uncheckOldFiat(position) }
    }

    private fun uncheckOldFiat(position: Int) {
        notifyItemChanged(currentCheckedPosition)
        currentCheckedPosition = position
        tapAction(fiats.indexOf(fiats[position]))
    }
}

class FiatViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = FiatListRowBinding.bind(view)

    private val fiatMap = StringArrayMapper.mapStringArray(view.resources.getStringArray(R.array.currencies))

    fun setData(fiat: String, currentPosition: Int, action: () -> Unit) {
        binding.apply {
            fiatName.text = prepareFiatHeader(fiat)
            fiatRadioButton.isChecked = adapterPosition == currentPosition
            fiatRow.setOnClickListener {
                if (adapterPosition != currentPosition) {
                    fiatRadioButton.isChecked = true
                    action()
                }
            }
        }
    }

    private fun prepareFiatHeader(fiat: String): String =
        fiatMap[fiat]?.let {
            String.format(FIAT_HEADER_FORMAT, it, fiat)
        }.orElse { fiat }

    companion object {
        private const val FIAT_HEADER_FORMAT = "%s (%s)"
    }
}