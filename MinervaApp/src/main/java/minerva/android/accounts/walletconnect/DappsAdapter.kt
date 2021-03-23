package minerva.android.accounts.walletconnect

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.DappItemBinding
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.walletconnect.DappSession

class DappsAdapter(private val disconnect: (peerId: String) -> Unit) :
    RecyclerView.Adapter<DappViewHolder>() {

    private var dapps: List<DappSession> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappViewHolder =
        DappViewHolder(
            DappItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { peerId -> disconnect(peerId) }

    override fun onBindViewHolder(holder: DappViewHolder, position: Int) {
        holder.setItem(dapps[position])
    }

    override fun getItemCount(): Int = dapps.size

    fun updateDapps(dappSessions: List<DappSession>) {
        this.dapps = dappSessions
        notifyDataSetChanged()
    }
}

@SuppressLint("RestrictedApi")
class DappViewHolder(
    private val binding: DappItemBinding,
    private val disconnect: (peerId: String) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {

    fun setItem(dappSession: DappSession) {
        with(binding) {
            Glide.with(binding.root.context)
                .load(getIcon(dappSession))
                .into(icon)
            name.text = dappSession.name
            menu.setOnClickListener { showMenu(dappSession.peerId) }
        }
    }

    private fun getIcon(dappSession: DappSession): Any =
        if (dappSession.iconUrl != String.Empty) dappSession.iconUrl else R.drawable.ic_services

    private fun DappItemBinding.showMenu(peerId: String) {
        PopupMenu(root.context, menu).apply {
            inflate(R.menu.dapp_menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.disconnect) disconnect(peerId)
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