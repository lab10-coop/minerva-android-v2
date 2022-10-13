package minerva.android.cryptographyProvider.repository.model

interface Seed

data class SeedWithKeys(
    val seed: String,
    val password: String,
    val publicKey: String,
    val privateKey: String
) : Seed

data class SeedError(val error: Throwable) : Seed