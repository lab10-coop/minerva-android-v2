package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.INCOGNITO_IDENTITY
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.INCOGNITO_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.INCOGNITO_PHONE

open class Identity(
    open val index: Int,
    open var name: String = String.Empty,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    open val data: LinkedHashMap<String, String> = linkedMapOf(),
    val isDeleted: Boolean = false,
    var isSelected: Boolean = false
) {
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
        Pair(IdentityField.PHONE_NUMBER, INCOGNITO_PHONE)
    )
) : Identity(index = Int.InvalidIndex)