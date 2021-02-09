package minerva.android.widget

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.KeyEvent
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSignMessageDialogBinding
import minerva.android.walletmanager.model.DappSession

class DappSignMessageDialog(context: Context, approve: () -> Unit, deny: () -> Unit) : DappDialog(context) {

    private val binding = DappSignMessageDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        setCancelable(false)

        with(binding) {
            buttons.setView({ approve() }, { deny() }, { dismiss() })
            message.body.apply {
                maxLines = 8
                movementMethod = ScrollingMovementMethod()
                isNestedScrollingEnabled = true
                isVerticalScrollBarEnabled = true
                isScrollbarFadingEnabled = false
                gravity = Gravity.CENTER_VERTICAL
                setLineSpacing(1f, 1f)
            }
        }
    }

    fun setContent(text: String, session: DappSession) = with(binding) {
        message.setTitleAndBody(context.getString(R.string.message), text)
        accountType.setNetwork(session)
        setupHeader(session.name, session.networkName, session.iconUrl)
    }
}