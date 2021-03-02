package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive

data class DappSession(
    override val address: String = String.Empty,
    val topic: String = String.Empty,
    val version: String = String.Empty,
    val bridge: String = String.Empty,
    val key: String = String.Empty,
    override var name: String = String.Empty,
    override val iconUrl: String = String.Empty,
    val peerId: String = String.Empty,
    val remotePeerId: String? = String.Empty,
    val networkName: String = String.Empty,
    val accountName: String = String.Empty,
    val chainId: Int = Int.InvalidValue
) : MinervaPrimitive(address = address, name = name, iconUrl = iconUrl)
