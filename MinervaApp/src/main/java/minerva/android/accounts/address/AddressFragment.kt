package minerva.android.accounts.address

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_address.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.widget.LetterLogo
import minerva.android.widget.setupCopyButton
import minerva.android.widget.setupShareButton
import minerva.android.wrapped.WrappedActivity.Companion.FRAGMENT_TYPE
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import minerva.android.wrapped.WrappedFragmentType
import net.glxn.qrgen.android.QRCode
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddressFragment : Fragment() {

    private val viewModel: AddressViewModel by viewModel()
    private val position: Int by lazy {
        arguments?.getInt(INDEX) ?: Int.InvalidIndex
    }
    private val fragmentType: WrappedFragmentType by lazy {
        arguments?.get(FRAGMENT_TYPE) as WrappedFragmentType
    }

    private var viewGroup: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewGroup = container
        return inflater.inflate(R.layout.fragment_address, container, false)
    }


    private fun initializeView(minervaPrimitive: MinervaPrimitive) {
        with(minervaPrimitive) {
            prepareLogo(this)
            prepareHeader(this)
            prepareQR(this)
            prepareAddress(this)
            setupShareButton(shareButton, prepareTextAddress(this))
            setupCopyButton(copyButton, prepareTextAddress(this), getString(R.string.address_saved_to_clip_board))
        }
    }

    private fun prepareLogo(minervaPrimitive: MinervaPrimitive) {
        letterLogo.apply {
            setImageDrawable(LetterLogo.createLogo(requireContext(), minervaPrimitive.name))
            visibleOrGone(isIdentity())
        }
    }

    private fun prepareHeader(minervaPrimitive: MinervaPrimitive) {
        header.apply {
            text = minervaPrimitive.name
            visibleOrGone(isIdentity())
        }
    }

    private fun prepareQR(minervaPrimitive: MinervaPrimitive) {
        resources.getDimensionPixelSize(R.dimen.qr_code_size).let { qrCodeSize ->
            with(prepareTextAddress(minervaPrimitive)) {
                if (isEmpty()) {
                    Toast.makeText(context, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                    return@let
                }
                val qr = QRCode.from(this).withSize(qrCodeSize, qrCodeSize).file()
                context?.let {
                    Glide.with(it).load(qr).into(qrCode)
                }
            }
        }
    }

    private fun prepareAddress(minervaPrimitive: MinervaPrimitive) {
        prepareTextAddress(minervaPrimitive).let { address ->
            textFullAddress.setTitleAndBody(prepareTitleAddress(), address)
            textShortAddress.apply {
                setTitleAndBody(prepareTitleAddress(), address)
                setSingleLine()
                setOnClickListener {
                    TransitionManager.beginDelayedTransition(viewGroup)
                    textFullAddress.visibleOrInvisible(!textFullAddress.isVisible)
                }
            }
        }
    }

    private fun initializeFragment() {
        with(viewModel) {
            loadMinervaPrimitiveLiveData.observe(this@AddressFragment, EventObserver { initializeView(it) })
            loadMinervaPrimitive(fragmentType, position)
        }
    }

    private fun isIdentity() = fragmentType == WrappedFragmentType.IDENTITY_ADDRESS

    private fun prepareTextAddress(minervaPrimitive: MinervaPrimitive) =
        if (isIdentity()) (minervaPrimitive as? Identity)?.did ?: String.Empty
        else minervaPrimitive.address

    private fun prepareTitleAddress() =
        if (isIdentity()) Identity.DID_LABEL
        else String.Empty

    companion object {
        @JvmStatic
        fun newInstance(fragmentType: WrappedFragmentType, index: Int) =
            AddressFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FRAGMENT_TYPE, fragmentType)
                    putInt(INDEX, index)
                }
            }
    }
}
