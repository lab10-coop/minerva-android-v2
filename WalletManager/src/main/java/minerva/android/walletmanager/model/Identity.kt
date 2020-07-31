package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_EMAIL
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_IDENTITY
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PHONE
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PRIVATE_KEY
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.INCOGNITO_PUBLIC_KEY
import minerva.android.walletmanager.model.defs.IdentityField

open class Identity(
    val index: Int,
    override var name: String = String.Empty,
    open var publicKey: String = String.Empty,
    open var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    open val personalData: LinkedHashMap<String, String> = linkedMapOf(),
    override var isDeleted: Boolean = false,
    val credentials: List<Credential> = listOf(),
    val services: List<Service> = listOf(),
    var isSelected: Boolean = false
) : MinervaPrimitive(publicKey, name, isDeleted) {
    constructor(index: Int, identity: Identity) : this(
        index,
        identity.name,
        identity.publicKey,
        identity.privateKey,
        identity.address,
        identity.personalData,
        identity.isDeleted,
        identity.credentials,
        identity.services,
        identity.isSelected
    )

    val did: String
        get() = DID_PREFIX + address

    override fun equals(other: Any?): Boolean = (other is Identity)
            && index == other.index
            && name == other.name
            && publicKey == other.publicKey
            && privateKey == other.privateKey
            && personalData == other.personalData
            && isDeleted == other.isDeleted
            && isSelected == other.isSelected

    companion object {
        const val DID_LABEL = "DID"
        private const val DID_PREFIX = "did:ethr:"
    }
}

data class IncognitoIdentity(
    override var name: String = INCOGNITO_IDENTITY,
    override val personalData: LinkedHashMap<String, String> = linkedMapOf(
        Pair(IdentityField.NAME, INCOGNITO_NAME),
        Pair(IdentityField.PHONE_NUMBER, INCOGNITO_PHONE),
        Pair(IdentityField.EMAIL, INCOGNITO_EMAIL)
    ),
    override var privateKey: String = INCOGNITO_PRIVATE_KEY,
    override var publicKey: String = INCOGNITO_PUBLIC_KEY
) : Identity(index = Int.InvalidIndex)