package minerva.android.settings.version

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.NoteRowBinding
import minerva.android.databinding.VersionRowBinding
import minerva.android.kotlinUtils.Empty

class ReleaseNoteAdapter(private val notes: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = notes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VERSION_ROW_TYPE -> VersionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.version_row, parent, false))
            else -> NotesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_row, parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        notes[position].run {
            if (isNoteRowType(this)) (holder as NotesViewHolder).bind(prepareNote(this))
            else (holder as VersionViewHolder).bind(this)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (isNoteRowType(notes[position])) NOTE_ROW_TYPE
        else VERSION_ROW_TYPE

    private fun isNoteRowType(note: String) = note.startsWith(NOTE_INDICATOR)

    private fun prepareNote(note: String) = note.replace(NOTE_INDICATOR, String.Empty).trim()

    companion object {
        private const val VERSION_ROW_TYPE = 3
        private const val NOTE_ROW_TYPE = 13
        private const val NOTE_INDICATOR = "\u2022"
    }

    inner class VersionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = VersionRowBinding.bind(view)

        fun bind(version: String) {
            binding.version.text = version
        }
    }

    inner class NotesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = NoteRowBinding.bind(view)

        fun bind(note: String) {
            binding.note.text = note
        }
    }
}

