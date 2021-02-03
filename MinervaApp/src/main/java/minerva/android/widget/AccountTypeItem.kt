package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import minerva.android.R
import minerva.android.databinding.AccountTypeItemBinding

class AccountTypeItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: AccountTypeItemBinding =
        AccountTypeItemBinding.bind(inflate(context, R.layout.account_type_item, this))

    fun setNetwork(name: String, account: String) = with(binding) {
        accountName.text = name
        address.text = account
    }
}