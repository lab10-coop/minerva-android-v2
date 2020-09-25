package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.CredentialsPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Credential

object CredentialsPayloadToCredentials : Mapper<List<CredentialsPayload>, List<Credential>> {
    override fun map(input: List<CredentialsPayload>): List<Credential> =
        mutableListOf<Credential>().apply {
            input.forEach { add(CredentialPayloadToCredentialMapper.map(it)) }
        }
}

object CredentialPayloadToCredentialMapper : Mapper<CredentialsPayload, Credential> {
    override fun map(input: CredentialsPayload): Credential =
        Credential(
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