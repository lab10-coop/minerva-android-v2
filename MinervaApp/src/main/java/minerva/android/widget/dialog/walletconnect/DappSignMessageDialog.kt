package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.databinding.DappDialogButtonsBinding
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSignMessageDialogBinding
import minerva.android.walletmanager.model.walletconnect.DappSession

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
            setLineSpacing(1f, 1f)
        }

    }

    fun setContent(text: String, session: DappSession) = with(binding) {
        message.setTitleAndBody(context.getString(R.string.message), text)
        accountType.setNetwork(session)
        setupHeader(session.name, session.networkName, session.iconUrl)
    }
}