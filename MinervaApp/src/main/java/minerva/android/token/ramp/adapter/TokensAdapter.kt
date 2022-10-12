package minerva.android.token.ramp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.RampCryptoRowBinding
import minerva.android.token.ramp.model.RampCrypto

class TokensAdapter(private var tokens: List<RampCrypto>) : RecyclerView.Adapter<TokenViewHolder>() {

    private var currentCryptoPosition = FIRST_TOKEN
    var onTokenSelected: (chainId: Int, symbol: String) -> Unit = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder =
        TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ramp_crypto_row, parent, false))

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {

        holder.apply {
            setData(position, tokens[position]) { onTokenSelection(position) }
            showSelected(tokens[position].isSelected)
        }
    }

    private fun onTokenSelection(position: Int) {
        currentCryptoPosition = position
        onTokenSelected(tokens[position].chainId, tokens[position].apiSymbol)
        notifyDataSetChanged()
    }

    fun updateTokens(onTokenSelected: (chainId: Int, symbol: String) -> Unit) {
        this.onTokenSelected = onTokenSelected
    }

    override fun getItemCount(): Int = tokens.size

    companion object {
        private const val FIRST_TOKEN = 0
    }
}

class TokenViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    private val binding = RampCryptoRowBinding.bind(view)

    fun setData(position: Int, rampCrypto: RampCrypto, onTokenSelection: (chainId: Int) -> Unit) = with(binding) {

        network.text = String.format(NETWORK_FORMAT, rampCrypto.network)
        tokenSymbol.apply {
            text = rampCrypto.displaySymbol
            setCompoundDrawablesWithIntrinsicBounds(NO_IMAGE, rampCrypto.iconRes, NO_IMAGE, NO_IMAGE)
        }
        cryptoView.setOnClickListener { onTokenSelection(position) }
    }

    fun showSelected(isSelected: Boolean) {
        binding.cryptoView.setBackgroundResource(
            if (isSelected) R.drawable.rounded_white_frame_purple
            else R.drawable.rounded_white_button
        )
    }

    companion object {
        private const val NO_IMAGE = 0
        private const val NETWORK_FORMAT = "(%s)"
    }

}
