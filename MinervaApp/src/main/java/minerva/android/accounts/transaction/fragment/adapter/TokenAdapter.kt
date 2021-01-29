package minerva.android.accounts.transaction.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import minerva.android.databinding.SpinnerTokenBinding
import minerva.android.walletmanager.model.token.Token

class TokenAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val tokens: List<Token>) :
    ArrayAdapter<Token>(context, layoutResource, tokens) {

    override fun getCount(): Int = tokens.size

    override fun getItem(position: Int): Token = tokens[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = convertView ?: createView(position, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        convertView ?: createView(position, parent)

    private fun createView(position: Int, parent: ViewGroup): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            SpinnerTokenBinding.bind(this).apply {
                tokenName.text = tokens[position].symbol
                tokenLogo.initView(tokens[position])
            }
        }
}