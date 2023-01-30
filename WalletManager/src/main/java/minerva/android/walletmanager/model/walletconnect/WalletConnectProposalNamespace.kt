package minerva.android.walletmanager.model.walletconnect

import com.walletconnect.sign.client.Sign

data class WalletConnectProposalNamespace(
    val chains: List<String>,
    val methods: List<String>,
    val events: List<String>,
    val extensions: List<Sign.Model.Namespace.Proposal.Extension>?,
)
