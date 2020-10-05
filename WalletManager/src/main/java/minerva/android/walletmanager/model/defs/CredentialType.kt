package minerva.android.walletmanager.model.defs

enum class CredentialType(val type: String) {
    DEFAULT("Default"),
    AUTOMOTIVE_CLUB("AutomotiveMembershipCardCredential"),
    VERIFIABLE_CREDENTIAL("VerifiableCredential")
}