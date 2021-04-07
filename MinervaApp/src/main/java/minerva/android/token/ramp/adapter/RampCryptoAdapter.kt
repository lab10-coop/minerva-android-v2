package minerva.android.token.ramp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.token.ramp.model.RampCrypto

class RampCryptoAdapter(private val crypto: List<RampCrypto>) : RecyclerView.Adapter<RampCryptoViewHolder>() {

    override fun getItemCount() = crypto.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RampCryptoViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.ramp_crypto_row, parent, false)
    )

    override fun onBindViewHolder(holder: RampCryptoViewHolder, position: Int) {
        holder.apply {
            setData(crypto[position])
            setListener()
            isSelected(false)
        }
    }
}

class RampCryptoViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun setData(rampCrypto: RampCrypto) {
        //TODO klop initialize element
    }

    fun setListener() {
        //TODO klop implement it
    }

    fun isSelected(value: Boolean) {
        //TODO klop implement it
    }
}