package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.Service

object ServiceToServicePayloadMapper : Mapper<Service, ServicePayload> {
    override fun map(input: Service): ServicePayload =
        with(input) { ServicePayload(issuer, name, lastUsed, loggedInIdentityPublicKey, iconUrl) }
}