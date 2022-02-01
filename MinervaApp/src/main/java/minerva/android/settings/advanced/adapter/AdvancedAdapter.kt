package minerva.android.settings.advanced.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.AdvancedSectionLayoutBinding
import minerva.android.settings.advanced.model.AdvancedSection
import minerva.android.settings.advanced.model.AdvancedSectionRowType

class AdvancedAdapter(
    private val onSettingPressed: (type: AdvancedSectionRowType) -> Unit
) : RecyclerView.Adapter<AdvancedViewHolder>() {

    var sections: List<AdvancedSection> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvancedViewHolder =
        AdvancedViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.advanced_section_layout, parent, false))

    override fun onBindViewHolder(holder: AdvancedViewHolder, position: Int) {
        holder.bindData(
            sections[position]
        ) { onSettingPressed(it) }
    }

    override fun getItemCount(): Int = sections.size

    fun updateList(sections: List<AdvancedSection>) {
        this.sections = sections
        notifyDataSetChanged()
    }
}

class AdvancedViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private var binding = AdvancedSectionLayoutBinding.bind(view)

    fun bindData(
        section: AdvancedSection,
        onSettingPressed: (type: AdvancedSectionRowType) -> Unit
    ) {
        view.run {
            binding.apply {
                setOnClickListener { onSettingPressed(section.rowType) }
                title.setText(section.title)
                description.setText(section.description)
                actionText.setText(section.actionText)
            }
        }
    }
}