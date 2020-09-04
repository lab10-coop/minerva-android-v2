package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
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
const val CARD_URL = "cardImage"
const val ICON_URL = "iconImage"
const val URL = "/"
const val GATEWAY = "http://ipfs-gateway.lab10.io"

fun mapHashMapToQrCodeResponse(map: Map<String, Any?>): QrCode {
    if (isVerifiableCredential(map)) {
        return when (getVCType(map)) {
            VerifiableCredentialType.AUTOMOTIVE_CLUB ->
                CredentialQrCode(
                    name = getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[CREDENTIAL_NAME] as String,
                    cardUrl = GATEWAY + (getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[CARD_URL] as Map<*, *>)[URL],
                    iconUrl = GATEWAY + (getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[ICON_URL] as Map<*, *>)[URL],
                    issuer = map[ISS] as String,
                    type = VerifiableCredentialType.AUTOMOTIVE_CLUB,
                    memberName = getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[NAME] as String,
                    memberId = getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[MEMBER_ID] as String,
                    coverage = getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[COVERAGE] as String,
                    expirationDate = map[EXP] as Long,
                    creationDate = getVerifiableCredentialsData(map, AUTOMOTIVE_MEMBERSHIP_CARD)[SINCE] as String,
                    loggedInDid = map[SUB] as String,
                    lastUsed = DateUtils.timestamp
                )
            else -> CredentialQrCode() //todo should return Credential object instead of CredentialQrCode() ?
        }
    } else {
        //todo should check if it is service qr code response type?
        return ServiceQrCode(
            issuer = map[ISS] as String,
            serviceName = getServiceName(map[ISS] as String),
            callback = map[CALLBACK] as String?,
            requestedData = getRequestedData(map),
            identityFields = getIdentityRequestedFields(getRequestedData(map))
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
        ServicesResponseToServicesMapper.map(response.services)
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