package minerva.android.walletmanager.model.mappers

import com.google.gson.Gson
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.minervaprimitives.credential.CredentialRequest
import minerva.android.walletmanager.model.minervaprimitives.credential.CredentialRequirements
import minerva.android.walletmanager.model.minervaprimitives.service.RequestedService

object CredentialRequestMapper : Mapper<Map<String, Any?>, CredentialRequest> {
    override fun map(input: Map<String, Any?>): CredentialRequest {
        val requirements = input["credentialRequirements"] as ArrayList<*>
        return CredentialRequest(
            issuer = input["iss"] as String,
            callbackUrl = input["callbackURL"] as String,
            credentialRequirements = Gson().fromJson(requirements[0] as String, CredentialRequirements::class.java),
            service = Gson().fromJson(input["service"] as String, RequestedService::class.java),
            type = input["typ"] as String
        )
    }
}