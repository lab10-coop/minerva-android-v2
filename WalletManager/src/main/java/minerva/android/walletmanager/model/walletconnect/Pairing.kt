package minerva.android.walletmanager.model.walletconnect

import com.walletconnect.android.Core
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive

data class Pairing(
    override val address: String = String.Empty,
    val topic: String = String.Empty,
    override var name: String = String.Empty,
    override val iconUrl: String = String.Empty,
    override val peerId: String = String.Empty,
    override val accountName: String = String.Empty,
    override val chainId: Int = Int.InvalidValue,
    val metaData: Core.Model.AppMetaData? = null
) : MinervaPrimitive(address = address, name = name, iconUrl = iconUrl)
