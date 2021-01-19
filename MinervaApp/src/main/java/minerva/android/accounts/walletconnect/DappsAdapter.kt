package minerva.android.accounts.walletconnect

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.DappItemBinding
import minerva.android.kotlinUtils.Empty

class DappsAdapter(private var dapps: List<Dapp>, private val disconnect: () -> Unit) :
    RecyclerView.Adapter<DappViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappViewHolder =
        DappViewHolder(
            DappItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { disconnect() }

    override fun onBindViewHolder(holder: DappViewHolder, position: Int) {
        holder.setItem(dapps[position])
    }

    override fun getItemCount(): Int = dapps.size

    fun updateDapps(dapps: List<Dapp>) {
        this.dapps = dapps
        notifyDataSetChanged()
    }
}

@SuppressLint("RestrictedApi")
class DappViewHolder(private val binding: DappItemBinding, private val disconnect: () -> Unit) :
    RecyclerView.ViewHolder(binding.root) {

    fun setItem(dapp: Dapp) {
        with(binding) {
            Glide.with(binding.root.context)
                .load(getIcon(dapp))
                .into(icon)
            name.text = dapp.name
            menu.setOnClickListener { showMenu() }
        }
    }

    private fun getIcon(dapp: Dapp): Any =
        if (dapp.icon != String.Empty) dapp.icon else R.drawable.ic_services

    private fun DappItemBinding.showMenu() {
        PopupMenu(root.context, menu).apply {
            inflate(R.menu.dapp_menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.disconnect) disconnect()
                true
            }
        }.also {
            with(MenuPopupHelper(root.context, it.menu as MenuBuilder, binding.menu)) {
                setForceShowIcon(true)
                gravity = Gravity.END
                show()
            }
        }
    }
}
