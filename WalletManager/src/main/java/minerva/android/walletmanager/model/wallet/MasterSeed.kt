package minerva.android.walletmanager.model.wallet

import minerva.android.kotlinUtils.Empty

interface MasterKeys

data class MasterSeedError(val error: Throwable) : MasterKeys

data class MasterSeed(
    private val _seed: String = String.Empty,
    private val _publicKey: String = String.Empty,
    private val _privateKey: String = String.Empty
) : MasterKeys {
    val seed: String get() = _seed
    val privateKey: String get() = _privateKey
    val publicKey: String get() = _publicKey
}