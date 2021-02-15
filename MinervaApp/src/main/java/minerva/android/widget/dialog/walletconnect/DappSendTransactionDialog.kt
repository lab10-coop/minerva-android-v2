package minerva.android.widget.dialog.walletconnect

import android.content.Context
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding

class DappSendTransactionDialog(context: Context, approve: () -> Unit, deny: () -> Unit) : DappDialog(context) {

    private val binding: DappSendTransactionDialogBinding = DappSendTransactionDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)

    }

}