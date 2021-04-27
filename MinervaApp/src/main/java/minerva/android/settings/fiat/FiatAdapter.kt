package minerva.android.settings.fiat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.FiatListRowBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
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
        holder.setData(fiats, currentCheckedPosition, position) { uncheckOldFiat(position) }
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

    fun setData(fiat: List<String>, currentlyChosen: Int, position: Int, action: () -> Unit) {
        binding.apply {
            prepareRow(fiatRow, position, fiat.size)
            fiatName.text = fiat[position]
            fiatFullName.text = prepareFiatFullName(fiat[position])
            checkButton.isEnabled = adapterPosition == currentlyChosen
            fiatRow.setOnClickListener {
                if (adapterPosition != currentlyChosen) {
                    checkButton.isEnabled = true
                    action()
                }
            }
        }
    }

    private fun prepareRow(fiatRow: ConstraintLayout, position: Int, fiatSize: Int) {
        fiatRow.apply {
            resources.getDimension(R.dimen.margin_big).toInt().let { bigPadding ->
                when (position) {
                    FIRST_ELEMENT -> {
                        setPadding(NO_PADDING, bigPadding, NO_PADDING, NO_PADDING)
                        setBackgroundResource(R.drawable.top_rounded_white_background)
                    }
                    fiatSize - 1 -> {
                        setPadding(NO_PADDING, NO_PADDING, NO_PADDING, bigPadding)
                        setBackgroundResource(R.drawable.bottom_rounded_white_background)
                    }
                    else -> {
                        setPadding(NO_PADDING, NO_PADDING, NO_PADDING, NO_PADDING)
                        setBackgroundResource(R.color.white)
                    }
                }
                binding.separator.visibleOrGone(position == FIRST_NOT_PROMOTED_FIAT_INDEX)
            }
        }
    }

    private fun prepareFiatFullName(fiat: String): String = fiatMap[fiat] ?: String.Empty

    companion object {
        private const val NO_PADDING = 0
        private const val FIRST_ELEMENT = 0
        private const val FIRST_NOT_PROMOTED_FIAT_INDEX = 3
    }
}