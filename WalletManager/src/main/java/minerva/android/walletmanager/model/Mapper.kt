package minerva.android.walletmanager.model

import minerva.android.configProvider.model.*

const val CALLBACK = "callback"
const val ISS = "iss"
const val REQUESTED = "requested"

fun mapHashMapToQrCodeResponse(responseMap: Map<String, Any?>): QrCodeResponse = QrCodeResponse(
    callback = responseMap[CALLBACK] as String?,
    issuer = responseMap[ISS] as String?,
    requestedData = getRequestedData(responseMap),
    isQrCodeValid = true
)

private fun getRequestedData(responseMap: Map<String, Any?>): ArrayList<String> {
    return if (responseMap[REQUESTED] is ArrayList<*>?) responseMap[REQUESTED] as ArrayList<String> else arrayListOf()
}

fun mapWalletConfigResponseToWalletConfig(response: WalletConfigResponse): WalletConfig {
    val identities = mutableListOf<Identity>()
    val values = mutableListOf<Value>()
    val services = mutableListOf<Service>()

    response.walletPayload.identityResponse.forEach {
        identities.add(mapIdentityPayloadToIdentity(it))
    }

    response.walletPayload.valueResponse.forEach {
        values.add(mapValueResponseToValue(it))
    }

    response.walletPayload.serviceResponse.forEach {
        services.add(mapServiceResponseToService(it))
    }
    return WalletConfig(response.walletPayload.version, identities, values, services)
}

fun mapServiceResponseToService(response: ServicePayload): Service =
    Service(response.type, response.name, response.lastUsed)

fun mapValueResponseToValue(response: ValuePayload): Value =
    Value(response.index, name = response.name, network = response.network)

fun mapIdentityPayloadToIdentity(response: IdentityPayload): Identity =
    Identity(response.index, response.name, data = response.data, isDeleted = response.isDeleted)

fun mapWalletConfigToWalletPayload(config: WalletConfig): WalletConfigPayload {
    val idResponses = mutableListOf<IdentityPayload>()
    val valResponses = mutableListOf<ValuePayload>()
    val servicesResponse = mutableListOf<ServicePayload>()

    config.identities.forEach {
        idResponses.add(mapIdentityToIdentityPayload(it))
    }

    config.values.forEach {
        valResponses.add(mapValueToValuePayload(it))
    }

    config.services.forEach {
        servicesResponse.add(mapServiceToServicePayload(it))
    }
    return WalletConfigPayload(config.version, idResponses, valResponses, servicesResponse)
}

fun mapServiceToServicePayload(service: Service): ServicePayload =
    ServicePayload(service.type, service.name, service.lastUsed)


fun mapIdentityToIdentityPayload(identity: Identity): IdentityPayload =
    IdentityPayload(identity.index, identity.name, identity.data, identity.isDeleted)

fun mapValueToValuePayload(value: Value): ValuePayload = ValuePayload(value.index, value.name, value.network)
