package minerva.android.token.ramp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import minerva.android.databinding.SpinnerTokenBinding
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class AccountAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val accounts: List<Account>) :
        ArrayAdapter<Account>(context, layoutResource, accounts) {

    override fun getCount(): Int = accounts.size

    override fun getItem(position: Int): Account = accounts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = convertView ?: createView(position, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            convertView ?: createView(position, parent)

    private fun createView(position: Int, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
                //TODO klop finish it!
//                SpinnerTokenBinding.bind(this).apply {
//                    tokenName.text = account[position].symbol
//                    tokenLogo.initView(tokens[position])
//                }
            }
}