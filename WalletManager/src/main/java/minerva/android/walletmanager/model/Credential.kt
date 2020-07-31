package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

//TODO complete model with correct fields from qr code
data class Credential(
    override var name: String = String.Empty,
    override val type: String = String.Empty,
    var lastUsed: String = String.Empty
) : MinervaPrimitive(name = name, type = type)