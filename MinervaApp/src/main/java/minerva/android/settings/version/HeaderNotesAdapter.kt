package minerva.android.settings.version

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R

class HeaderNotesAdapter : RecyclerView.Adapter<HeaderNotesAdapter.HeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HeaderViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.notes_header, parent, false)
    )

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {}

    override fun getItemCount(): Int = HEADER_COUNT

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val HEADER_COUNT = 1
    }
}