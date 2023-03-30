package minerva.android.walletmanager.model.walletconnect

import com.walletconnect.android.Core
import com.walletconnect.sign.client.Sign
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletConnect.model.session.WCSession

data class DappSessionV2(
    override val address: String = String.Empty,
    override val topic: String = String.Empty,
    override val version: String = WCSession.VERSION_2,
    override var name: String = String.Empty,
    override val iconUrl: String = String.Empty,
    override val peerId: String = String.Empty,
    override val networkName: String = String.Empty,
    override val accountName: String = String.Empty,
    override val chainId: Int = Int.InvalidValue,
    override val isMobileWalletConnect: Boolean = false,
    val metaData: Core.Model.AppMetaData? = null,
    val namespaces: Map<String, Sign.Model.Namespace.Session>? = null
) : DappSession(address = address, name = name, iconUrl = iconUrl, version = version, topic = topic, isMobileWalletConnect = isMobileWalletConnect)
