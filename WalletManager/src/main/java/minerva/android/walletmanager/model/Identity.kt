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
    open val data: LinkedHashMap<String, String> = linkedMapOf(),
    override var isDeleted: Boolean = false,
    var isSelected: Boolean = false
): Account(publicKey, name, isDeleted) {
    constructor(index: Int, identity: Identity) : this(
        index,
        identity.name,
        identity.publicKey,
        identity.privateKey,
        identity.data,
        identity.isDeleted,
        identity.isSelected
    )

    override fun equals(other: Any?): Boolean = (other is Identity)
            && index == other.index
            && name == other.name
            && publicKey == other.publicKey
            && privateKey == other.privateKey
            && data == other.data
            && isDeleted == other.isDeleted
            && isSelected == other.isSelected
}

data class IncognitoIdentity(
    override var name: String = INCOGNITO_IDENTITY,
    override val data: LinkedHashMap<String, String> = linkedMapOf(
        Pair(IdentityField.NAME, INCOGNITO_NAME),
        Pair(IdentityField.PHONE_NUMBER, INCOGNITO_PHONE),
        Pair(IdentityField.EMAIL, INCOGNITO_EMAIL)
    ),
    override var privateKey: String = INCOGNITO_PRIVATE_KEY,
    override var publicKey: String = INCOGNITO_PUBLIC_KEY
) : Identity(index = Int.InvalidIndex)