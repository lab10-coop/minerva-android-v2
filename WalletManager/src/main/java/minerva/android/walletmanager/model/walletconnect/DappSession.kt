package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive

open class DappSession(
    override val address: String = String.Empty,
    open val topic: String = String.Empty,
    open val version: String = String.Empty,
    override var name: String = String.Empty,
    override val iconUrl: String = String.Empty,
    override val peerId: String = String.Empty,
    open val networkName: String = String.Empty,
    override val accountName: String = String.Empty,
    override val chainId: Int = Int.InvalidValue,
    open val isMobileWalletConnect: Boolean = false
) : MinervaPrimitive(address = address, name = name, iconUrl = iconUrl)
