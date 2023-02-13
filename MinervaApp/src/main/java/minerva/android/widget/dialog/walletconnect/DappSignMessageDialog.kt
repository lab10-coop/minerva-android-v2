package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSignMessageDialogBinding
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.walletconnect.DappSessionV2

class DappSignMessageDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding = DappSignMessageDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
        binding.message.body.apply {
            maxLines = 8
            movementMethod = ScrollingMovementMethod()
            isNestedScrollingEnabled = true
            isVerticalScrollBarEnabled = true
            isScrollbarFadingEnabled = false
            gravity = Gravity.CENTER_VERTICAL
            setLineSpacing(ADD, MULT)
        }

    }

    fun setContent(text: String, session: DappSessionV1) = with(binding) {
        message.setTitleAndBody(context.getString(R.string.message), text)
        accountType.setNetwork(session.accountName, session.address, session.chainId)
        setupHeader(session.name, session.networkName, session.iconUrl)
    }

    // todo: implement and move somewhere reasonable
    fun getNetworkNameFromChainId(chainId: Int): String = ""

    fun setContentV2(text: String, session: DappSessionV2) = with(binding) {
        message.setTitleAndBody(context.getString(R.string.message), text)
        accountType.setNetwork(session.accountName, session.address, session.chainId)
        val networkName = getNetworkNameFromChainId(session.chainId)
        setupHeader(session.name, networkName, session.iconUrl)
    }

    companion object {
        private const val ADD = 1f
        private const val MULT = 1f
    }
}