package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Identity

object IdentityToIdentityPayloadMapper: Mapper<Identity, IdentityPayload> {
    override fun map(input: Identity): IdentityPayload =
        IdentityPayload(
            input.index,
            input.name,
            input.personalData,
            input.isDeleted,
            ServicesToServicesPayloadMapper.map(input.services)
        )
}