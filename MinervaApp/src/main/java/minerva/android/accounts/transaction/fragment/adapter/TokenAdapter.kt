package minerva.android.accounts.transaction.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import minerva.android.R
import minerva.android.databinding.SpinnerDropdownViewBinding
import minerva.android.databinding.SpinnerTokenBinding
import minerva.android.extension.visibleOrGone
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.TokenWithBalances
import minerva.android.walletmanager.utils.BalanceUtils.getCryptoBalance
import minerva.android.walletmanager.utils.BalanceUtils.getFiatBalance
import minerva.android.widget.repository.getNetworkIcon

class TokenAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val tokens: List<TokenWithBalances>,
    private val account: Account,
    private val fiatSymbol: String
) : ArrayAdapter<TokenWithBalances>(context, layoutResource, tokens) {

    override fun getCount(): Int = tokens.size

    override fun getItem(position: Int): TokenWithBalances = tokens[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = convertView ?: createView(position, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createDropDownView(position, parent)

    private fun createView(position: Int, parent: ViewGroup): View =
        LayoutInflater.from(context).inflate(layoutResource, parent, false).apply {
            SpinnerTokenBinding.bind(this).apply {
                val item = tokens[position]
                item.token.let { token ->
                    tokenName.text = token.symbol
                    tokenLogo.initView(token)
                    networkLogo.setImageDrawable(getNetworkIcon(context, token.chainId, account.isSafeAccount))
                    arrow.visibleOrGone(count > ONE_ELEMENT)
                }
                amountView.apply {
                    setCrypto(getCryptoBalance(item.balance))
                    setFiat(getFiatBalance(item.fiatBalance, fiatSymbol))
                }
            }
        }


    private fun createDropDownView(position: Int, parent: ViewGroup): View =
        LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_view, parent, false).apply {
            SpinnerDropdownViewBinding.bind(this).apply {
                val token = tokens[position].token
                tokenName.text = token.symbol
                tokenLogo.initView(token)
                networkLogo.setImageDrawable(getNetworkIcon(context, token.chainId, account.isSafeAccount))
            }
        }

    companion object {
        private const val ONE_ELEMENT = 1
    }
}