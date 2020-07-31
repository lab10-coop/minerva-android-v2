package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.configProvider.model.walletConfig.*
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
    Identity(
        response.index,
        response.name,
        publicKey,
        privateKey,
        address,
        response.data,
        response.isDeleted,
        mapCredentialPayloadToCredentials(response.credentials),
        mapServicesResponseToServices(response.services)
    )

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


fun mapServicesResponseToServices(responses: List<ServicePayload>): List<Service> =
    mutableListOf<Service>().apply {
        responses.forEach { add(Service(it.type, it.name, it.lastUsed, it.loggedInIdentityPublicKey)) }
    }

private fun mapCredentialPayloadToCredentials(responses: List<CredentialsPayload>): List<Credential> =
    mutableListOf<Credential>().apply {
        responses.forEach { add(Credential(it.name, it.type, it.lastUsed)) }
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
        identity.personalData,
        identity.isDeleted,
        mapCredentialToCredentialsPayload(identity.credentials),
        mapServicesToServicesPayload(identity.services)

    )

fun mapServicesToServicesPayload(responses: List<Service>): List<ServicePayload> =
    mutableListOf<ServicePayload>().apply {
        responses.forEach { add(ServicePayload(it.type, it.name, it.lastUsed, it.loggedInIdentityPublicKey)) }
    }

fun mapCredentialToCredentialsPayload(responses: List<Credential>): List<CredentialsPayload> =
    mutableListOf<CredentialsPayload>().apply {
        responses.forEach { add(CredentialsPayload(it.name, it.type, it.lastUsed)) }
    }

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