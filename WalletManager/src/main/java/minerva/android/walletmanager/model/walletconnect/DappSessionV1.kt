package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class DappSessionV1(
    override val address: String = String.Empty,
    override val topic: String = String.Empty,
    override val version: String = "1",
    val bridge: String = String.Empty,
    val key: String = String.Empty,
    override var name: String = String.Empty,
    override val iconUrl: String = String.Empty,
    override val peerId: String = String.Empty,
    val remotePeerId: String? = String.Empty,
    val networkName: String = String.Empty,
    override val accountName: String = String.Empty,
    override val chainId: Int = Int.InvalidValue,
    val handshakeId: Long = Long.InvalidValue,
    override val isMobileWalletConnect: Boolean = false
) : DappSession(address = address, name = name, iconUrl = iconUrl, version = version, topic = topic, isMobileWalletConnect = isMobileWalletConnect)
