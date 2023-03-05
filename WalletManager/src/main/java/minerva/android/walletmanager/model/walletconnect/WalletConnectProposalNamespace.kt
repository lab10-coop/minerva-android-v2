package minerva.android.walletmanager.model.walletconnect

data class WalletConnectProposalNamespace(
    val chains: List<String>,
    val methods: List<String>,
    val events: List<String>
)
