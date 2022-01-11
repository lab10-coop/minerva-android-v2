package minerva.android.services.dapps.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import minerva.android.R
import minerva.android.databinding.DappListRowBinding
import minerva.android.services.dapps.model.Dapp

class DappsAdapter(private val listener: Listener) :
    ListAdapter<Dapp, DappsAdapter.ViewHolder>(Dapp.DIFF_CALLBACK) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            DappListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    interface Listener {
        fun onDappSelected(onDappSelected: OnDappSelected)
        data class OnDappSelected(val dapp: Dapp)
    }

    class ViewHolder(
        private val binding: DappListRowBinding,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dapp: Dapp) {
            with(binding) {
                dappName.text = dapp.shortName
                dappDescription.text = dapp.description
                dappIcon.loadUrl(dapp.iconUrl)
                mainContent.apply {
                    setBackgroundColor(Color.parseColor(dapp.colorHex))
                    setOnClickListener { listener.onDappSelected(Listener.OnDappSelected(dapp)) }
                }
            }
        }

        private fun ImageView.loadUrl(url: String) {
            val imageLoader = context.imageLoader
            val request = ImageRequest.Builder(this.context)
                .error(R.drawable.white_circle_placeholder)
                .data(url)
                .target(this)
                .build()
            imageLoader.enqueue(request)
        }
    }
}

