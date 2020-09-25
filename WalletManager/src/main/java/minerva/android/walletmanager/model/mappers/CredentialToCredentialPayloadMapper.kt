package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.CredentialsPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Credential

object CredentialToCredentialPayloadMapper : Mapper<Credential, CredentialsPayload> {
    override fun map(input: Credential): CredentialsPayload =
        CredentialsPayload(
            input.name,
            input.type,
            input.issuer,
            input.token,
            input.memberName,
            input.memberId,
            input.coverage,
            input.expirationDate,
            input.creationDate,
            input.loggedInIdentityDid,
            input.lastUsed,
            input.cardUrl,
            input.iconUrl
        )
}