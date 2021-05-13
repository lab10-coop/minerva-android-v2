package minerva.android.token.ramp.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.databinding.SpinnerNetworkBinding
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.repository.getNetworkIcon

class AccountSpinnerAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val accounts: List<Account>) :
        ArrayAdapter<Account>(context, layoutResource, accounts) {

    override fun getCount(): Int = accounts.size

    override fun getItem(position: Int): Account = accounts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = convertView ?: createView(position, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            convertView ?: createView(position, parent)

    private fun createView(position: Int, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
                accounts[position].let { account ->
                    SpinnerNetworkBinding.bind(this).row.apply {
                        text = if (account.id == Int.InvalidId) {
                            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                            gravity = Gravity.CENTER
                            context.getString(R.string.create_new_account)
                        } else {
                            setCompoundDrawablesWithIntrinsicBounds(getNetworkIcon(context, account.chainId), null, null, null)
                            account.name
                        }
                    }
                }
            }
}