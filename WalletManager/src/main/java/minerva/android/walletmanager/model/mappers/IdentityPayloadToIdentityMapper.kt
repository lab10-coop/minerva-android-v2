package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Identity

object IdentityPayloadToIdentityMapper {
    fun map(
        response: IdentityPayload,
        publicKey: String = String.Empty,
        privateKey: String = String.Empty,
        address: String = String.Empty
    ): Identity =
        Identity(
            response.index,
            response.name,
            publicKey,
            privateKey,
            address,
            response.data,
            response.isDeleted,
            ServicesResponseToServicesMapper.map(response.services)
        )
}