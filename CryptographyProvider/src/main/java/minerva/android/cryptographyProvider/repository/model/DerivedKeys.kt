package minerva.android.cryptographyProvider.repository.model

data class DerivedKeys(
    val index: Int,
    val publicKey: String,
    val privateKey: String,
    val address: String,
    val isTestNet: Boolean = true
)