package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.CredentialQrCode

object CredentialQrCodeToCredentialMapper : Mapper<CredentialQrCode, Credential> {
    override fun map(input: CredentialQrCode): Credential =
        Credential(
            name = input.name,
            type = input.type.type,
            membershipType = input.membershipType.type,
            issuer = input.issuer,
            token = input.token,
            creationDate = input.creationDate,
            expirationDate = input.expirationDate,
            coverage = input.coverage,
            memberId = input.memberId,
            memberName = input.memberName,
            lastUsed = input.lastUsed,
            loggedInIdentityDid = input.loggedInDid,
            cardUrl = input.cardUrl,
            iconUrl = input.iconUrl
        )
}