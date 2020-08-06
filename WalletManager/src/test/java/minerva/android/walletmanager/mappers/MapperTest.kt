package minerva.android.walletmanager.mappers

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class MapperTest : WalletConfigTestValues() {

    private val credentialQrCodeResult = mapOf<String, Any?>(
        "vc" to mapOf(
            "type" to arrayListOf("VerifiableCredential", "AutomotiveMembershipCardCredential"),
            "credentialSubject" to mapOf(
                "automotiveMembershipCard" to mapOf(
                    "memberId" to "123456",
                    "since" to "2018",
                    "coverage" to "touring",
                    "name" to "name"
                )
            )
        ),
        "iss" to "did:ethr:01016a194e4d5beee3a634edb156f84d03354a03",
        "exp" to 1234L,
        "sub" to "loggedDid"
    )

    private val credentialQrCodeResultWithDifferentVCType = mapOf<String, Any?>(
        "vc" to mapOf(
            "type" to arrayListOf("VerifiableCredential", "differentType"),
            "credentialSubject" to mapOf(
                "automotiveMembershipCard" to mapOf(
                    "memberId" to "123456",
                    "name" to "name",
                    "since" to "2018",
                    "coverage" to "touring"
                )
            )
        ),
        "iss" to "did:ethr:01016a194e4d5beee3a634edb156f84d03354a03",
        "exp" to 1234L,
        "sub" to "loggedDid"
    )

    private val serviceQrCodeResult = mapOf<String, Any?>(
        "iss" to "did:ethr:0x95b200870916377a74fc65d628a735d58bc22c98",
        "exp" to 1234L,
        "sub" to "loggedDid",
        "requested" to arrayListOf("test1", "test2"),
        "callback" to "callback"
    )

    @Test
    fun `Mapping Identity to IdentityResponse Test`() {
        val identity = Identity(
            0,
            "IdentityTest",
            "publicKey",
            "privateKey",
            "address",
            identityData,
            true
        )
        val identityResponse = mapIdentityToIdentityPayload(identity)

        identity.index shouldBeEqualTo identityResponse.index
        identity.name shouldBeEqualTo identityResponse.name
        identity.personalData shouldBeEqualTo identityResponse.data
    }

    @Test
    fun `Mapping IdentityResponse to Identity Test`() {
        val identityResponse = IdentityPayload(
            0,
            "IdentityResponseTest",
            identityData,
            true
        )
        val identity = mapIdentityPayloadToIdentity(identityResponse)

        identity.index shouldBeEqualTo identityResponse.index
        identity.name shouldBeEqualTo identityResponse.name
        identity.personalData shouldBeEqualTo identityResponse.data
    }

    @Test
    fun `Mapping Value to ValueResponse Test`() {
        val value = Account(
            0,
            "publicKey",
            "privateKey",
            "ValueTest",
            "ValueNetworkTest"
        )

        val valueResponse = mapAccountToAccountPayload(value)

        value.index shouldBeEqualTo valueResponse.index
        value.name shouldBeEqualTo valueResponse.name
        value.network shouldBeEqualTo value.network
    }

    @Test
    fun `Mapping ValueResponse to Value Test`() {
        val valueResponse = AccountPayload(
            0,
            "ValueResponseTest",
            "ValueNetworkTest"
        )

        val value = mapAccountResponseToAccount(valueResponse)

        value.index shouldBeEqualTo valueResponse.index
        value.name shouldBeEqualTo valueResponse.name
        value.network shouldBeEqualTo value.network
    }

    @Test
    fun `Mapping WalletConfig to WalletPayload Test`() {
        val walletConfig = WalletConfig(
            0,
            identities,
            values
        )
        val walletPayload = mapWalletConfigToWalletPayload(walletConfig)

        walletConfig.version shouldBeEqualTo walletPayload.version
        walletConfig.identities[0].name shouldBeEqualTo walletPayload.identityResponse[0].name
        walletConfig.accounts[0].name shouldBeEqualTo walletPayload.accountResponse[0].name
    }

    @Test
    fun `map qr code result to credential qr code response`() {
        val result = mapHashMapToQrCodeResponse(credentialQrCodeResult)
        assert(result is CredentialQrResponse)
        (result as CredentialQrResponse).run {
            memberName == "name" &&
                    name == "ÖAMTC-Member Card" &&
                    issuer == "did:ethr:01016a194e4d5beee3a634edb156f84d03354a03" &&
                    type == "AutomotiveMembershipCardCredential" &&
                    memberId == "123456" &&
                    creationDate == "2018" &&
                    coverage == "touring"
        }
    }

    @Test
    fun `map qr code result to credential qr code response with no vc type`() {
        val result = mapHashMapToQrCodeResponse(credentialQrCodeResultWithDifferentVCType)
        assert(result is CredentialQrResponse)
        (result as CredentialQrResponse).run {
            name == "" &&
                    memberId == "" &&
                    creationDate == "" &&
                    coverage == ""
        }
    }

    @Test
    fun `map qr code result to service qr code response`() {
        val result = mapHashMapToQrCodeResponse(serviceQrCodeResult)
        assert(result is ServiceQrResponse)
        (result as ServiceQrResponse).run {
            serviceName == "Demo Web Page Login" &&
                    callback == "callback" &&
                    requestedData[0] == "test1" &&
                    identityFields == "test1 test2" &&
                    issuer == "did:ethr:0x95b200870916377a74fc65d628a735d58bc22c98" &&
                    identityFields == "touring"
        }
    }
}