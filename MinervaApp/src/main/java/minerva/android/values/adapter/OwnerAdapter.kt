package minerva.android.values.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.owner_list_row.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.values.listener.OnOwnerRemovedListener

class OwnerAdapter(private val listener: OnOwnerRemovedListener) : RecyclerView.Adapter<OwnerViewHolder>(),
    OwnerViewHolder.OnOwnerRemovedListener {

    private var owners = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnerViewHolder =
        OwnerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.owner_list_row, parent, false),
            this@OwnerAdapter
        )

    override fun getItemCount(): Int = owners.size

    override fun onBindViewHolder(holder: OwnerViewHolder, position: Int) {
        holder.apply {
            setData(position, owners[position])
        }
    }

    fun updateList(data: List<String>) {
        owners = data
        notifyDataSetChanged()
    }

    override fun onOwnerRemoved(position: Int) {
        listener.onOwnerRemoved(owners[position])
    }
}

class OwnerViewHolder(private val view: View, private val onOwnerRemovedListener: OnOwnerRemovedListener) : RecyclerView.ViewHolder(view) {

    fun setData(position: Int, owner: String) {
        view.apply {
            ownerAddress.text = owner
            setOnMenuClickListener(position)
            ownerRowMenu.visibleOrGone(position != MASTER_OWNER_INDEX)
        }
    }

    private fun View.setOnMenuClickListener(position: Int) {
        ownerRowMenu.setOnClickListener { showMenu(position, ownerRowMenu) }
    }

    private fun showMenu(position: Int, anchor: View): Boolean {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.owner_menu, menu)
            gravity = Gravity.RIGHT
            show()
            setOnItemMenuClickListener(position)
        }
        return true
    }

    private fun PopupMenu.setOnItemMenuClickListener(position: Int) {
        setOnMenuItemClickListener {
            if(it.itemId == R.id.remove) onOwnerRemovedListener.onOwnerRemoved(position)
            true
        }
    }

    interface OnOwnerRemovedListener {
        fun onOwnerRemoved(position: Int)
    }

    companion object {
        private const val MASTER_OWNER_INDEX = 0
    }
}