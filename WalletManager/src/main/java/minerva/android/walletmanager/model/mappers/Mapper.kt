package minerva.android.walletmanager.model.mappers

import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.configProvider.model.walletConfig.*
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.ServiceName
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.model.defs.VerifiableCredentialType
import minerva.android.walletmanager.utils.DateUtils

const val CALLBACK = "callback"
const val ISS = "iss"
const val REQUESTED = "requested"
const val MEMBER_ID = "memberId"
const val NAME = "name"
const val CREDENTIAL_NAME = "credentialName"
const val COVERAGE = "coverage"
const val EXP = "exp"
const val SUB = "sub"
const val SINCE = "since"
const val VC = "vc"
const val CREDENTIAL_SUBJECT = "credentialSubject"
const val AUTOMOTIVE_MEMBERSHIP_CARD = "automotiveMembershipCard"
const val VERIFIABLE_CREDENTIAL = "VerifiableCredential"
const val TYPE = "type"

fun mapHashMapToQrCodeResponse(responseMap: Map<String, Any?>): QrCode {
    if (isVerifiableCredential(responseMap)) {
        return when (getVCType(responseMap)) {
            VerifiableCredentialType.AUTOMOTIVE_CLUB ->
                CredentialQrCode(
                    name = getVerifiableCredentialsData(responseMap, AUTOMOTIVE_MEMBERSHIP_CARD)[CREDENTIAL_NAME] as String,
                    issuer = responseMap[ISS] as String,
                    type = VerifiableCredentialType.AUTOMOTIVE_CLUB,
                    memberName = getVerifiableCredentialsData(responseMap, AUTOMOTIVE_MEMBERSHIP_CARD)[NAME] as String,
                    memberId = getVerifiableCredentialsData(responseMap, AUTOMOTIVE_MEMBERSHIP_CARD)[MEMBER_ID] as String,
                    coverage = getVerifiableCredentialsData(responseMap, AUTOMOTIVE_MEMBERSHIP_CARD)[COVERAGE] as String,
                    expirationDate = responseMap[EXP] as Long,
                    creationDate = getVerifiableCredentialsData(responseMap, AUTOMOTIVE_MEMBERSHIP_CARD)[SINCE] as String,
                    loggedInDid = responseMap[SUB] as String,
                    lastUsed = DateUtils.timestamp
                )
            else -> CredentialQrCode() //todo should return Credential object in stead of CredentialQrCode() ?
        }
    } else {
        //todo should check if it is service qr code response type?
        return ServiceQrCode(
            issuer = responseMap[ISS] as String,
            serviceName = getServiceName(responseMap[ISS] as String),
            callback = responseMap[CALLBACK] as String?,
            requestedData = getRequestedData(responseMap),
            identityFields = getIdentityRequestedFields(getRequestedData(responseMap))
        )
    }
}

private fun getIdentityRequestedFields(requestedData: ArrayList<String>): String {
    val identityFields = StringBuilder()
    requestedData.forEach { identityFields.append("$it ") }
    return identityFields.toString()
}

private fun getServiceName(issuer: String): String =
    when (issuer) {
        ServiceType.UNICORN_LOGIN -> ServiceName.UNICORN_LOGIN_NAME
        ServiceType.CHARGING_STATION -> ServiceName.CHARGING_STATION_NAME
        else -> String.Empty
    }

private fun getVCType(responseMap: Map<String, Any?>): Any? =
    ((responseMap[VC] as Map<*, *>)[TYPE] as ArrayList<*>)[1]

private fun isVerifiableCredential(responseMap: Map<String, Any?>): Boolean {
    responseMap[VC]?.let {
        ((responseMap[VC] as Map<*, *>)[TYPE] as ArrayList<*>).forEach {
            if (it == VERIFIABLE_CREDENTIAL) return true
        }
        return false
    }.orElse {
        return false
    }
}

private fun getVerifiableCredentialsData(responseMap: Map<String, Any?>, type: String): Map<*, Any?> =
    ((responseMap[VC] as Map<*, Any?>)[CREDENTIAL_SUBJECT] as Map<*, Any?>)[type] as Map<*, Any?>

private fun getRequestedData(responseMap: Map<String, Any?>): ArrayList<String> =
    if (responseMap[REQUESTED] is ArrayList<*>?) responseMap[REQUESTED] as ArrayList<String>
    else arrayListOf()

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

fun mapWalletConfigToWalletPayload(config: WalletConfig): WalletConfigPayload {
    val idResponses = mutableListOf<IdentityPayload>()
    val accountsResponse = mutableListOf<AccountPayload>()
    val servicesResponse = mutableListOf<ServicePayload>()
    val credentialsResponse = mutableListOf<CredentialsPayload>()

    config.identities.forEach {
        idResponses.add(mapIdentityToIdentityPayload(it))
    }

    config.accounts.forEach {
        accountsResponse.add(mapAccountToAccountPayload(it))
    }

    config.services.forEach {
        servicesResponse.add(mapServiceToServicePayload(it))
    }

    config.credentials.forEach {
        credentialsResponse.add(mapCredentialToCredentialPayload(it))
    }
    return WalletConfigPayload(
        config.version,
        idResponses,
        accountsResponse,
        servicesResponse,
        credentialsResponse
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
        mapServicesToServicesPayload(identity.services)

    )

fun mapServicesToServicesPayload(responses: List<Service>): List<ServicePayload> =
    mutableListOf<ServicePayload>().apply {
        responses.forEach { add(ServicePayload(it.type, it.name, it.lastUsed, it.loggedInIdentityPublicKey)) }
    }

fun mapCredentialsPayloadToCredentials(responses: List<CredentialsPayload>): List<Credential> =
    mutableListOf<Credential>().apply {
        responses.forEach { add(mapCredentialPayloadToCredential(it)) }
    }

private fun mapCredentialPayloadToCredential(payload: CredentialsPayload): Credential =
    Credential(
        payload.name,
        payload.type,
        payload.issuer,
        payload.memberName,
        payload.memberId,
        payload.coverage,
        payload.expirationDate,
        payload.creationDate,
        payload.loggedInIdentityDid,
        payload.lastUsed,
        payload.isDeleted
    )

private fun mapCredentialToCredentialPayload(credential: Credential): CredentialsPayload =
    CredentialsPayload(
        credential.name,
        credential.type,
        credential.issuer,
        credential.memberName,
        credential.memberId,
        credential.coverage,
        credential.expirationDate,
        credential.creationDate,
        credential.loggedInIdentityDid,
        credential.lastUsed,
        credential.isDeleted
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

fun mapCredentialQrCodeToCredential(input: CredentialQrCode): Credential =
    Credential(
        name = input.name,
        type = input.type,
        issuer = input.issuer,
        creationDate = input.creationDate,
        expirationDate = input.expirationDate,
        coverage = input.coverage,
        memberId = input.memberId,
        memberName = input.memberName,
        lastUsed = input.lastUsed,
        loggedInIdentityDid = input.loggedInDid
    )