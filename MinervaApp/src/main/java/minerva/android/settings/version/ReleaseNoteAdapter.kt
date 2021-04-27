package minerva.android.settings.version

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.NotesRowBinding
import minerva.android.settings.model.ReleaseNote

class ReleaseNoteAdapter(private val notes: List<ReleaseNote>) : RecyclerView.Adapter<ReleaseNoteAdapter.NotesViewHolder>() {

    override fun getItemCount(): Int = notes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NotesViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.notes_row, parent, false)
    )

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        holder.setData(notes[position])
    }

    class NotesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = NotesRowBinding.bind(view)

        fun setData(note: ReleaseNote) {
            binding.apply {
                version.text = note.version
                body.text = note.note
            }
        }
    }
}

