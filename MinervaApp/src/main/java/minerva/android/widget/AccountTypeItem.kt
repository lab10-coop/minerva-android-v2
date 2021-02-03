package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.AccountTypeItemBinding
import minerva.android.walletmanager.model.DappSession
import minerva.android.widget.repository.getNetworkIcon

class AccountTypeItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: AccountTypeItemBinding =
        AccountTypeItemBinding.bind(inflate(context, R.layout.account_type_item, this))

    fun setNetwork(session: DappSession) = with(binding) {
        accountName.text = session.accountName
        address.text = session.address
        Glide.with(binding.root.context)
            .load(getNetworkIcon(context, session.networkShort))
            .into(logo)
    }
}