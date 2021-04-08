package minerva.android.token.ramp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.RampCryptoRowBinding
import minerva.android.token.ramp.listener.OnRampCryptoChangedListener
import minerva.android.token.ramp.listener.OnRampCryptoClickListener
import minerva.android.token.ramp.model.RampCrypto

class RampCryptoAdapter(private val crypto: List<RampCrypto>, private val listener: OnRampCryptoChangedListener) : RecyclerView.Adapter<RampCryptoViewHolder>(), OnRampCryptoClickListener {

    private var currentCryptoPosition = 0

    override fun getItemCount() = crypto.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RampCryptoViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.ramp_crypto_row, parent, false), this
    )

    override fun onBindViewHolder(holder: RampCryptoViewHolder, position: Int) {
        holder.apply {
            setData(position, crypto[position])
            showSelected(position == currentCryptoPosition)
        }
    }

    override fun onRampCryptoClicked(position: Int) {
        currentCryptoPosition = position
        listener.onRampCryptoChanged(crypto[position].chainId)
        notifyDataSetChanged()
    }
}

class RampCryptoViewHolder(view: View, private val listener: OnRampCryptoClickListener) : RecyclerView.ViewHolder(view) {

    private val binding = RampCryptoRowBinding.bind(view)

    fun setData(position: Int, rampCrypto: RampCrypto) {
        binding.cryptoView.apply {
            text = rampCrypto.symbol
            setCompoundDrawablesWithIntrinsicBounds(NO_IMAGE, rampCrypto.iconRes, NO_IMAGE, NO_IMAGE)
            setOnClickListener { listener.onRampCryptoClicked(position) }
        }
    }

    fun showSelected(value: Boolean) {
        binding.cryptoView.setBackgroundResource(
                if (value) R.drawable.rounded_white_frame_purple
                else R.drawable.rounded_white_button
        )
    }

    companion object {
        private const val NO_IMAGE = 0
    }
}