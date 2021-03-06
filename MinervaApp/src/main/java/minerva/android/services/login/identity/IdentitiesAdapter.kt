package minerva.android.services.login.identity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.painless_login_item.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.IncognitoIdentity

class IdentitiesAdapter : RecyclerView.Adapter<ItemViewHolder>(),
    ItemViewHolder.IdentitiesAdapterListener {

    private var identities = listOf<Identity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.painless_login_item, parent, false))

    override fun getItemCount(): Int = identities.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.setListener(this)
        holder.bindView(identities[position])
    }

    fun updateList(identities: MutableList<Identity>) {
        identities.add(IncognitoIdentity())
        this.identities = identities.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun getSelectedIdentity() = identities.find { it.isSelected }

    override fun onIdentityClicked(identity: Identity) {
        this.identities.forEach {
            if (it != identity) {
                it.isSelected = false
            }
        }
        notifyDataSetChanged()
    }
}

class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    private lateinit var listener: IdentitiesAdapterListener

    fun setListener(listener: IdentitiesAdapterListener) {
        this.listener = listener
    }

    fun bindView(identity: Identity) {
        identity.apply {
            view.checkButton.isEnabled = isSelected
            view.identityName.text = name
            setOnItemClickListener()
            loadIdentityLogo()
        }
    }

    private fun Identity.loadIdentityLogo() {
        if (this is IncognitoIdentity) {
            view.apply {
                letterLogo.invisible()
                incognitoLogo.visible()
            }
        } else {
            view.apply {
                letterLogo.visible()
                incognitoLogo.invisible()
                letterLogo.createLogo(name)
            }
        }
    }

    private fun Identity.setOnItemClickListener() {
        view.setOnClickListener {
            isSelected = !isSelected
            view.checkButton.isEnabled = isSelected
            listener.onIdentityClicked(this)
        }
    }

    interface IdentitiesAdapterListener {
        fun onIdentityClicked(identity: Identity)
    }
}

