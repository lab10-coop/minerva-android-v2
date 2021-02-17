package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.Service

object ServicesResponseToServicesMapper : Mapper<List<ServicePayload>, List<Service>> {
    override fun map(input: List<ServicePayload>): List<Service> =
        mutableListOf<Service>().apply {
            input.forEach { add(Service(it.issuer, it.name, it.lastUsed, it.loggedInIdentityPublicKey, it.iconUrl)) }
        }
}