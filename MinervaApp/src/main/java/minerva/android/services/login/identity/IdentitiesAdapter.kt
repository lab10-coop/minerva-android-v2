package minerva.android.services.login.identity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.painless_login_item.view.*
import minerva.android.R
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.IncognitoIdentity
import minerva.android.walletmanager.model.NewIdentity
import minerva.android.widget.ProfileImage

class IdentitiesAdapter : RecyclerView.Adapter<ItemViewHolder>(), ItemViewHolder.IdentitiesAdapterListener {

    private var identities = listOf<Identity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.painless_login_item, parent, false))

    override fun getItemCount(): Int = identities.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.setListener(this)
        holder.bindView(identities[position])
    }

    override fun onIdentityClicked(identity: Identity) {
        this.identities.forEach {
            if (it != identity) {
                it.isSelected = false
            }
        }
        notifyDataSetChanged()
    }

    fun updateList(identities: MutableList<Identity>) {
        identities.apply {
            add(NewIdentity())
            add(IncognitoIdentity())
        }
        this.identities = identities.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun getSelectedIdentity() = identities.find { it.isSelected }
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
            loadIdentityLogo(identity)
        }
    }

    private fun loadIdentityLogo(identity: Identity) {
        if (identity is IncognitoIdentity) {
            view.apply {
                profileImage.invisible()
                incognitoLogo.visible()
            }
        } else {
            view.apply {
                profileImage.visible()
                incognitoLogo.invisible()
                ProfileImage.load(profileImage, identity)
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

