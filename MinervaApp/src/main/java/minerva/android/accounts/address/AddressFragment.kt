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
import minerva.android.R
import minerva.android.databinding.FragmentAddressBinding
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.widget.ProfileImage
import minerva.android.widget.setupCopyButton
import minerva.android.widget.setupShareButton
import minerva.android.wrapped.WrappedActivity.Companion.FRAGMENT_TYPE
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import minerva.android.wrapped.WrappedFragmentType
import net.glxn.qrgen.android.QRCode
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddressFragment : Fragment(R.layout.fragment_address) {

    private lateinit var binding: FragmentAddressBinding

    private val viewModel: AddressViewModel by viewModel()
    private val index: Int by lazy {
        arguments?.getInt(INDEX) ?: Int.InvalidIndex
    }
    private val fragmentType: WrappedFragmentType by lazy {
        arguments?.get(FRAGMENT_TYPE) as WrappedFragmentType
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddressBinding.bind(view)
        initializeFragment()
    }

    private fun initializeView(minervaPrimitive: MinervaPrimitive) {
        with(minervaPrimitive) {
            prepareLogo(this)
            prepareHeader(this)
            prepareQR(this)
            prepareAddress(this)
            setupShareButton(binding.shareButton, prepareTextAddress(this))
            setupCopyButton(binding.copyButton, prepareTextAddress(this), prepareToastMessage())
        }
    }

    private fun prepareLogo(minervaPrimitive: MinervaPrimitive) {
        binding.profileImage.apply {
            (minervaPrimitive as? Identity)?.let {
                ProfileImage.load(this, it)
                visible()
                return
            }
            gone()
        }
    }

    private fun prepareHeader(minervaPrimitive: MinervaPrimitive) {
        binding.title.apply {
            (minervaPrimitive as? Identity)?.let {
                text = it.identityTitle
            }
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
                    Glide.with(it).load(qr).into(binding.qrCode)
                }
            }
        }
    }

    private fun prepareAddress(minervaPrimitive: MinervaPrimitive) {
        binding.apply {
            textFullAddress.setTitleAndBody(prepareTitleAddress(), prepareTextAddress(minervaPrimitive))
            textShortAddress.apply {
                setTitleAndBody(prepareTitleAddress(), prepareTextAddress(minervaPrimitive))
                setSingleLine()
                setOnClickListener {
                    TransitionManager.beginDelayedTransition(addressView)
                    textFullAddress.visibleOrInvisible(!textFullAddress.isVisible)
                }
            }
        }
    }

    private fun initializeFragment() {
        with(viewModel) {
            loadMinervaPrimitiveLiveData.observe(this@AddressFragment, EventObserver { initializeView(it) })
            loadMinervaPrimitive(fragmentType, index)
        }
    }

    private fun isIdentity() = fragmentType == WrappedFragmentType.IDENTITY_ADDRESS

    private fun prepareTextAddress(minervaPrimitive: MinervaPrimitive) =
            if (isIdentity()) (minervaPrimitive as? Identity)?.did ?: String.Empty
            else minervaPrimitive.address

    private fun prepareToastMessage() =
            if (isIdentity()) getString(R.string.identity_saved_to_clipboard)
            else getString(R.string.address_saved_to_clip_board)

    private fun prepareTitleAddress() =
            if (isIdentity()) DID_LABEL
            else String.Empty

    companion object {
        const val DID_LABEL = "DID"

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