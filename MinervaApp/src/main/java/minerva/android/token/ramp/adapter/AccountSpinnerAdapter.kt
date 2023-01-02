package minerva.android.token.ramp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.spinner_network_add_item.view.*
import minerva.android.R
import minerva.android.accounts.AccountsFragment.Companion.ADD_ITEM
import minerva.android.accounts.AccountsFragment.Companion.ITEM
import minerva.android.databinding.SpinnerNetworkAddItemBinding
import minerva.android.databinding.SpinnerNetworkBinding
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.repository.getNetworkIcon

class AccountSpinnerAdapter(
    context: Context,
    private val accounts: List<Account>,
    private val numberOfAccountsToUse: Int
) :
    ArrayAdapter<Account>(context, 0, accounts) {

    override fun getItemViewType(position: Int): Int = when (accounts[position].id) {
        Int.InvalidId -> ADD_ITEM
        else -> ITEM
    }

    override fun getCount(): Int = accounts.size

    override fun getItem(position: Int): Account = accounts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createView(position, parent, true)

    private fun createView(position: Int, parent: ViewGroup, isDropDown: Boolean): View = when (getItemViewType(position)) { //getting type of layout
        ITEM -> { //token info(item) case
            LayoutInflater.from(context).inflate(R.layout.spinner_network, parent, false).apply {
                accounts[position].let { account ->
                    SpinnerNetworkBinding.bind(this).row.apply {
                        text = when {
                            account.id == Int.InvalidId && accounts.size > numberOfAccountsToUse -> {
                                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_light_error, NO_ICON, NO_ICON, NO_ICON)
                                context.getString(R.string.no_addresses_left_info)
                            } else -> {
                                val topDrawable = if(!isDropDown) ContextCompat.getDrawable(context,R.drawable.ic_dropdown_black) else null
                                setCompoundDrawablesWithIntrinsicBounds(getNetworkIcon(context, account.chainId), null, topDrawable, null)
                                account.name
                            }
                        }
                    }
                }
            }
        }
        ADD_ITEM -> { //add new account (button) case
            LayoutInflater.from(context).inflate(R.layout.spinner_network_add_item, parent, false).apply {
                accounts[position].let { _ ->
                    SpinnerNetworkAddItemBinding.bind(this).rowContainer.apply{
                        add_account_row.text = context.getString(R.string.create_new_account)
                    }
                }
            }
        }
        else -> error(context.getString(R.string.unknown_type))
    }

    companion object {
        private const val NO_ICON = 0
    }
}