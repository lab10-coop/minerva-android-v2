package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Service

object ServicesToServicesPayloadMapper : Mapper<List<Service>, List<ServicePayload>> {
    override fun map(input: List<Service>): List<ServicePayload> =
        mutableListOf<ServicePayload>().apply {
            input.forEach { add(ServicePayload(it.issuer, it.name, it.lastUsed, it.loggedInIdentityPublicKey, it.iconUrl)) }
        }
}