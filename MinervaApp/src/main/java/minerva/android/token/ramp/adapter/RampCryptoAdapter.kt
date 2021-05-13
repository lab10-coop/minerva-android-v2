package minerva.android.token.ramp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.RampCryptoRowBinding
import minerva.android.token.ramp.model.RampCrypto

class RampCryptoAdapter(private val crypto: List<RampCrypto>, private val onRampChanged: (chainId: Int) -> Unit) :
    RecyclerView.Adapter<RampCryptoViewHolder>() {

    private var currentCryptoPosition = 0

    override fun getItemCount() = crypto.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RampCryptoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ramp_crypto_row, parent, false))

    override fun onBindViewHolder(holder: RampCryptoViewHolder, position: Int) {
        holder.apply {
            setData(position, crypto[position]) { onRampClicked(it) }
            showSelected(position == currentCryptoPosition)
        }
    }

    private fun onRampClicked(position: Int) {
        currentCryptoPosition = position
        onRampChanged(crypto[position].chainId)
        notifyDataSetChanged()
    }

    fun getCryptoPosition() = currentCryptoPosition
}

class RampCryptoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = RampCryptoRowBinding.bind(view)

    fun setData(position: Int, rampCrypto: RampCrypto, onRampClicked: (chainId: Int) -> Unit) {
        binding.cryptoView.apply {
            text = rampCrypto.symbol
            setCompoundDrawablesWithIntrinsicBounds(NO_IMAGE, rampCrypto.iconRes, NO_IMAGE, NO_IMAGE)
            setOnClickListener { onRampClicked(position) }
        }
    }

    fun showSelected(value: Boolean) {
        binding.cryptoView.setBackgroundResource(
            if (value) R.drawable.rounded_white_frame_purple
            else R.drawable.rounded_white_button
        )
    }

    companion object {
        const val DEFAULT_RAMP_CRYPTO_POSITION = 0
        private const val NO_IMAGE = 0
    }
}