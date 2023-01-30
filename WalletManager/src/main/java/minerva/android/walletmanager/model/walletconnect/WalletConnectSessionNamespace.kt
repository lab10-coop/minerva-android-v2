package minerva.android.walletmanager.model.walletconnect

import com.walletconnect.sign.client.Sign

data class WalletConnectSessionNamespace(
    val accounts: List<String>,
    val methods: List<String>,
    val events: List<String>,
    val extensions: List<Sign.Model.Namespace.Session.Extension>?,
)
