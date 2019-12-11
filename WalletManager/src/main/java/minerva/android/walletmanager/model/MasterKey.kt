package minerva.android.walletmanager.model

import minerva.android.Empty

open class MasterKey(
    private val _publicKey: String = String.Empty,
    private val _privateKey: String = String.Empty
) {
    val publicKey: String
        get() = _publicKey
    val privateKey: String
        get() = _privateKey
}