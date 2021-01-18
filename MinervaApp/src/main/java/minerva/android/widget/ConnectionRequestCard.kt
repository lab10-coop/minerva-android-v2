package minerva.android.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import minerva.android.R
import minerva.android.databinding.ConnectionRequestCardLayoutBinding
import minerva.android.extension.gone

class ConnectionRequestCard
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ConnectionRequestCardLayoutBinding =
        ConnectionRequestCardLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun setRequestedData(data: String) {
        binding.requested.text = data
    }

    fun hideRequestedData() = with(binding) {
        requested.gone()
        requestedDataLabel.gone()
    }

    fun setConnectionIcon(resId: Int) {
        binding.connectionView.setConnectionIcon(resId)
    }

    fun setIcon(url: String) {
        binding.connectionView.setIconUrl(Uri.parse(url))
    }

    fun setDefaultIcon() {
        binding.connectionView.setConnectionIcon(R.drawable.ic_services)
    }

    fun setConnectionIconsPadding() {
        binding.connectionView.setConnectionIconsPadding()
    }
}