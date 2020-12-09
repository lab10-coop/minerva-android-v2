package minerva.android.services.login.identity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.PainlessLoginItemBinding
import minerva.android.extension.visibleOrInvisible
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
        }
        this.identities = identities.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun getSelectedIdentity() = identities.find { it.isSelected }
}

class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private lateinit var listener: IdentitiesAdapterListener
    private var binding = PainlessLoginItemBinding.bind(view)

    fun setListener(listener: IdentitiesAdapterListener) {
        this.listener = listener
    }

    fun bindView(identity: Identity) {
        identity.apply {
            binding.apply {
                checkButton.isEnabled = isSelected
                identityName.text = name
            }
            setOnItemClickListener()
            loadIdentityLogo(identity)
        }
    }

    private fun loadIdentityLogo(identity: Identity) {
        binding.apply {
            profileImage.visibleOrInvisible(identity is IncognitoIdentity)
            incognitoLogo.visibleOrInvisible(identity is IncognitoIdentity)
            if (identity !is IncognitoIdentity) ProfileImage.load(profileImage, identity)
        }
    }

    private fun Identity.setOnItemClickListener() {
        view.setOnClickListener {
            isSelected = !isSelected
            binding.checkButton.isEnabled = isSelected
            listener.onIdentityClicked(this)
        }
    }

    interface IdentitiesAdapterListener {
        fun onIdentityClicked(identity: Identity)
    }
}

