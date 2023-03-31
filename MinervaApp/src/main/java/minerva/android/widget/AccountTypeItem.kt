package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.AccountTypeItemBinding
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.walletconnect.DappSessionV2
import minerva.android.widget.repository.getNetworkIcon

class AccountTypeItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: AccountTypeItemBinding =
        AccountTypeItemBinding.bind(inflate(context, R.layout.account_type_item, this))

    fun setNetwork(_accountName: String, _address: String, chainId: Int) = with(binding) {
        accountName.text = _accountName
        address.text = _address
        Glide.with(root.context)
            .load(getNetworkIcon(context, chainId))
            .into(logo)
    }
}