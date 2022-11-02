package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.DialogSelectPredefinedAccountBinding
import minerva.android.walletmanager.model.defs.ChainId

class SelectPredefinedAccountDialog(context: Context, private val predefinedNetworkOnClick: (Int) -> Unit) :
    Dialog(context, R.style.DialogStyle) {

    private val binding = DialogSelectPredefinedAccountBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        initView()
    }

    private fun initView() = with(binding) {
        skip.setOnClickListener { doOnSelectedNetwork(ChainId.ETH_MAIN) }
        ethereumNetwork.setOnClickListener { doOnSelectedNetwork(ChainId.ETH_MAIN) }
        xdaiNetwork.setOnClickListener { doOnSelectedNetwork(ChainId.GNO) }
        celoNetwork.setOnClickListener { doOnSelectedNetwork(ChainId.CELO) }
    }

    private fun doOnSelectedNetwork(chainId: Int) {
        predefinedNetworkOnClick(chainId)
        dismiss()
    }
}