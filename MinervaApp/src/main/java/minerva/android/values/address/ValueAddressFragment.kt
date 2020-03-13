package minerva.android.values.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_value_address.*
import minerva.android.R
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Value
import minerva.android.widget.setupCopyButton
import minerva.android.widget.setupShareButton
import net.glxn.qrgen.android.QRCode
import org.koin.androidx.viewmodel.ext.android.viewModel

class ValueAddressFragment : Fragment() {

    private val viewModel: ValueAddressViewModel by viewModel()
    private var position: Int = Int.InvalidIndex

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_value_address, container, false)

    private fun initializeView(value: Value) {
        value.address.run {
            prepareQR(this)
            valueAddress.text = this
            setupShareButton(shareButton, this)
            setupCopyButton(copyButton, this, getString(R.string.address_saved_to_clip_board))
        }
    }

    private fun prepareQR(address: String) {
        resources.getDimensionPixelSize(R.dimen.qr_code_size).let { qrCodeSize ->
            if (address.isEmpty()) {
                Toast.makeText(context, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                return@let
            }
            val qr = QRCode.from(address).withSize(qrCodeSize, qrCodeSize).file()
            context?.let {
                Glide.with(it).load(qr).into(qrCode)
            }
        }
    }

    private fun initializeFragment() {
        viewModel.loadValueLiveData.observe(this, EventObserver { initializeView(it) })
        arguments?.let {
            position = it.getInt(INDEX)
            viewModel.loadValue(position)
        }
    }

    companion object {
        private const val INDEX = "index"

        @JvmStatic
        fun newInstance(index: Int) =
            ValueAddressFragment().apply {
                arguments = Bundle().apply {
                    putInt(INDEX, index)
                }
            }
    }
}
