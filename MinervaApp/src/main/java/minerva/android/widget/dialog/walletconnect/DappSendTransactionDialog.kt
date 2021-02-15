package minerva.android.widget.dialog.walletconnect

import android.content.Context
import minerva.android.databinding.DappNetworkHeaderBinding
import minerva.android.databinding.DappSendTransactionDialogBinding
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

class DappSendTransactionDialog(context: Context, approve: () -> Unit, deny: () -> Unit) :
    DappDialog(context, { approve() }, { deny() }) {

    private val binding: DappSendTransactionDialogBinding = DappSendTransactionDialogBinding.inflate(layoutInflater)
    override val networkHeader: DappNetworkHeaderBinding = DappNetworkHeaderBinding.bind(binding.root)

    init {
        setContentView(binding.root)
        initButtons(binding.confirmationButtons)
    }

    fun setContent(transaction: WalletConnectTransaction, session: DappSession) = with(binding) {
        setupHeader(session.name, session.networkName, session.iconUrl)
    }
}