package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.*

const val CALLBACK = "callback"
const val ISS = "iss"
const val REQUESTED = "requested"

fun mapHashMapToQrCodeResponse(responseMap: Map<String, Any?>): QrCodeResponse =
    QrCodeResponse(
        callback = responseMap[CALLBACK] as String?,
        issuer = responseMap[ISS] as String,
        requestedData = getRequestedData(responseMap)
    )

private fun getRequestedData(responseMap: Map<String, Any?>): ArrayList<String> {
    return if (responseMap[REQUESTED] is ArrayList<*>?) responseMap[REQUESTED] as ArrayList<String> else arrayListOf()
}

fun mapIdentityPayloadToIdentity(
    response: IdentityPayload,
    publicKey: String = String.Empty,
    privateKey: String = String.Empty,
    address: String = String.Empty
): Identity =
    Identity(response.index, response.name, publicKey, privateKey, address, response.data, response.isDeleted)

fun mapAccountResponseToAccount(
    response: AccountPayload,
    publicKey: String = String.Empty,
    privateKey: String = String.Empty,
    address: String = String.Empty
): Account =
    Account(
        response.index,
        publicKey,
        privateKey,
        address,
        response.name,
        response.network,
        response.isDeleted,
        owners = response.owners,
        contractAddress = response.contractAddress,
        bindedOwner = response.bindedOwner
    )

private fun mapServiceResponseToService(response: ServicePayload): Service =
    Service(response.type, response.name, response.lastUsed, response.loggedInIdentityPublicKey)

fun mapServicesResponseToServices(responses: List<ServicePayload>): List<Service> {
    val services = mutableListOf<Service>()
    responses.forEach { services.add(mapServiceResponseToService(it)) }
    return services
}

fun mapWalletConfigToWalletPayload(config: WalletConfig): WalletConfigPayload {
    val idResponses = mutableListOf<IdentityPayload>()
    val valResponses = mutableListOf<AccountPayload>()
    val servicesResponse = mutableListOf<ServicePayload>()

    config.identities.forEach {
        idResponses.add(mapIdentityToIdentityPayload(it))
    }

    config.accounts.forEach {
        valResponses.add(mapAccountToAccountPayload(it))
    }

    config.services.forEach {
        servicesResponse.add(mapServiceToServicePayload(it))
    }
    return WalletConfigPayload(
        config.version,
        idResponses,
        valResponses,
        servicesResponse
    )
}

fun mapServiceToServicePayload(service: Service): ServicePayload =
    ServicePayload(service.type, service.name, service.lastUsed, service.loggedInIdentityPublicKey)


fun mapIdentityToIdentityPayload(identity: Identity): IdentityPayload =
    IdentityPayload(
        identity.index,
        identity.name,
        identity.data,
        identity.isDeleted
    )

fun mapAccountToAccountPayload(account: Account): AccountPayload =
    AccountPayload(
        account.index,
        account.name,
        account.network,
        account.isDeleted,
        account.owners,
        account.contractAddress,
        account.bindedOwner
    )

fun mapTransactionToTransactionPayload(transaction: Transaction): TransactionPayload =
    transaction.run {
        TransactionPayload(
            address,
            privateKey,
            receiverKey,
            amount,
            gasPrice,
            gasLimit,
            contractAddress
        )
    }

fun mapTransactionCostPayloadToTransactionCost(payload: TransactionCostPayload): TransactionCost =
    payload.run { TransactionCost(gasPrice, gasLimit, cost) }