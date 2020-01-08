package minerva.android.walletmanager.model

import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.configProvider.model.WalletConfigPayload

fun mapIdentityPayloadToIdentity(response: IdentityPayload): Identity =
    Identity(response.index, response.name, data = response.data, isDeleted = response.isDeleted)

fun mapIdentityToIdentityResponse(identity: Identity): IdentityPayload =
    IdentityPayload(identity.index, identity.name, identity.data, identity.isDeleted)

fun mapValueResponseToValue(response: ValuePayload): Value = Value(response.index, name = response.name, network = response.network)

fun mapValueToValueResponse(value: Value): ValuePayload = ValuePayload(value.index, value.name, value.network)

fun mapWalletConfigResponseToWalletConfig(response: WalletConfigResponse): WalletConfig {
    val identities = mutableListOf<Identity>()
    val values = mutableListOf<Value>()

    response.walletPayload.identityResponses.forEach {
        identities.add(mapIdentityPayloadToIdentity(it))
    }

    response.walletPayload.valueResponses.forEach {
        values.add(mapValueResponseToValue(it))
    }
  return WalletConfig(response.walletPayload.version, identities, values)
}

fun mapWalletConfigToWalletPayload(config: WalletConfig): WalletConfigPayload {
    val idResponses = mutableListOf<IdentityPayload>()
    val valResponses = mutableListOf<ValuePayload>()

    config.identities.forEach {
        idResponses.add(mapIdentityToIdentityResponse(it))
    }

    config.values.forEach {
        valResponses.add(mapValueToValueResponse(it))
    }
    return WalletConfigPayload(config.version, idResponses, valResponses)
}