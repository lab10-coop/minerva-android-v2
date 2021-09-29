package minerva.android.token.ramp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.RampCryptoGridBinding
import minerva.android.token.ramp.model.RampCrypto

class RampCryptoAdapter(
    private var tokens: List<RampCrypto>,
    private val onTokenSelected: (chainId: Int, symbol: String) -> Unit
) : RecyclerView.Adapter<RampCryptoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RampCryptoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ramp_crypto_grid, parent, false))

    override fun onBindViewHolder(holder: RampCryptoViewHolder, position: Int) {
        when (position) {
            FIRST_PAGE -> holder.setTokens(tokens.subList(0, 4))
            SECOND_PAGE -> holder.setTokens(tokens.subList(4, 8))
            THIRD_PAGE -> holder.setTokens(tokens.subList(8, 10))
        }
    }

    private fun RampCryptoViewHolder.setTokens(tokens: List<RampCrypto>) {
        setData(TokensAdapter(tokens)) { chainId, symbol -> updateTokens(chainId, symbol) }
    }

    private fun updateTokens(chainId: Int, symbol: String) {
        onTokenSelected(chainId, symbol)
        tokens.forEach { token -> token.isSelected = false }
        tokens.find { token -> token.chainId == chainId && token.symbol == symbol }?.isSelected = true
    }

    override fun getItemCount() = PAGES

    fun notifyData() {
        notifyDataSetChanged()
    }

    companion object {
        private const val FIRST_PAGE = 0
        private const val SECOND_PAGE = 1
        private const val THIRD_PAGE = 2
        private const val PAGES = 3
    }
}

class RampCryptoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = RampCryptoGridBinding.bind(view)

    fun setData(tokensAdapter: TokensAdapter, onTokenSelected: (chainId: Int, symbol: String) -> Unit) {
        tokensAdapter.updateTokens { chainId, symbol -> onTokenSelected(chainId, symbol) }
        binding.cryptoRecycler.apply {
            layoutManager = GridLayoutManager(context, RAMP_CRYPTO_COLUMNS)
            adapter = tokensAdapter
        }
    }

    companion object {
        private const val RAMP_CRYPTO_COLUMNS = 2
    }

}
