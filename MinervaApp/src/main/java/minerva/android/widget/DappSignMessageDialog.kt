package minerva.android.widget

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.KeyEvent
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSignMessageDialogBinding
import minerva.android.walletmanager.model.DappSession

class DappSignMessageDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    private val binding = DappSignMessageDialogBinding.inflate(layoutInflater)
    private val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        setCancelable(false)

        with(binding) {
            cancel.setOnClickListener {
                deny()
                dismiss()
            }
            connect.setOnClickListener {
                approve()
                dismiss()
            }

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
        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                deny()
                dismiss()
            }
            true
        }
    }

    fun setContent(text: String, session: DappSession) = with(binding) {
        message.setTitleAndBody(context.getString(R.string.message), text)
        accountType.setNetwork(session)
        networkHeader.name.text = session.name
        networkHeader.network.text = session.networkName
        Glide.with(binding.root.context)
            .load(session.iconUrl)
            .into(networkHeader.icon)
    }
}