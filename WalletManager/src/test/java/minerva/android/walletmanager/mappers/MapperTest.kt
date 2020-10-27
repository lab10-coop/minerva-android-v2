package minerva.android.walletmanager.mappers

import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.configProvider.model.walletConfig.CredentialsPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class MapperTest : WalletConfigTestValues() {

    private val credentialQrCodeResult = mapOf<String, Any?>(
        "vc" to mapOf(
            "type" to arrayListOf("VerifiableCredential", "AutomotiveMembershipCardCredential"),
            "credentialSubject" to mapOf(
                "automotiveMembershipCard" to mapOf(
                    "memberId" to "123456",
                    "since" to "2018",
                    "coverage" to "touring",
                    "name" to "name",
                    "credentialName" to "card",
                    "cardImage" to mapOf("/" to "urlImage"),
                    "iconImage" to mapOf("/" to "urlIcon")
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
        "callback" to "callback",
        "iconImage" to mapOf("/" to "urlIcon"),
        "name" to "name"
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
        val identityResponse = IdentityToIdentityPayloadMapper.map(identity)

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
    fun `Mapping Account to AccountResponse Test`() {
        val value = Account(
            0,
            "publicKey",
            "privateKey",
            "ValueTest",
            "ValueNetworkTest"
        )

        val valueResponse = AccountToAccountPayloadMapper.map(value)

        value.index shouldBeEqualTo valueResponse.index
        value.name shouldBeEqualTo valueResponse.name
        value.network shouldBeEqualTo value.network
    }

    @Test
    fun `mapping credential qr code to credential test`() {
        val input = CredentialQrCode(
            "iss",
            "name",
            "type"
        )

        val response = CredentialQrCodeToCredentialMapper.map(input)

        input.name shouldBeEqualTo response.name
        input.issuer shouldBeEqualTo response.issuer
    }

    @Test
    fun `mapping credentials payload code to credentials test`() {
        val input = listOf(
            CredentialsPayload(
                "name",
                "type",
                "iss"
            )
        )

        val response = CredentialsPayloadToCredentials.map(input)

        input[0].name shouldBeEqualTo response[0].name
        input[0].issuer shouldBeEqualTo response[0].issuer
        input[0].type shouldBeEqualTo response[0].type
    }

    @Test
    fun `mapping credential payload code to credential test`() {
        val input =
            CredentialsPayload(
                "name",
                "type",
                "iss"
            )

        val response = CredentialPayloadToCredentialMapper.map(input)

        input.name shouldBeEqualTo response.name
        input.issuer shouldBeEqualTo response.issuer
        input.type shouldBeEqualTo response.type
    }

    @Test
    fun `mapping credential code to credential payload test`() {
        val input =
            Credential(
                "name",
                "type",
                "iss"
            )

        val response = CredentialToCredentialPayloadMapper.map(input)

        input.name shouldBeEqualTo response.name
        input.issuer shouldBeEqualTo response.issuer
        input.type shouldBeEqualTo response.type
    }

    @Test
    fun `Mapping ValueResponse to Value Test`() {
        accountsResponse[0].let {
            NetworkManager.initialize(networks)
            val value = mapAccountResponseToAccount(it)

            value.index shouldBeEqualTo it.index
            value.name shouldBeEqualTo it.name
            value.network shouldBeEqualTo value.network
        }
    }

    @Test
    fun `map service response to service Test`() {
        val input = listOf(
            ServicePayload(
                "1",
                "name",
                123
            )
        )

        val response = ServicesResponseToServicesMapper.map(input)

        response[0].issuer shouldBeEqualTo input[0].issuer
        response[0].name shouldBeEqualTo input[0].name
        response[0].lastUsed shouldBeEqualTo input[0].lastUsed
    }

    @Test
    fun `map services to services payload Test`() {
        val input = listOf(
            Service(
                "1",
                "name",
                123
            )
        )

        val response = ServicesToServicesPayloadMapper.map(input)

        response[0].issuer shouldBeEqualTo input[0].issuer
        response[0].name shouldBeEqualTo input[0].name
        response[0].lastUsed shouldBeEqualTo input[0].lastUsed
    }

    @Test
    fun `map service to service payload Test`() {
        val input = Service(
            "1",
            "name",
            123
        )

        val response = ServiceToServicePayloadMapper.map(input)

        response.issuer shouldBeEqualTo input.issuer
        response.name shouldBeEqualTo input.name
        response.lastUsed shouldBeEqualTo input.lastUsed
    }

    @Test
    fun `map transaction cost payload to transaction cost test`() {
        val input = TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)

        val response = TransactionCostPayloadToTransactionCost.map(input)

        response.gasLimit shouldBeEqualTo input.gasLimit
        response.gasPrice shouldBeEqualTo input.gasPrice
        response.cost shouldBeEqualTo input.cost
    }

    @Test
    fun `transaction mapper test`() {
        val input = Transaction(contractAddress = "address", privateKey = "private", receiverKey = "receiver")
        val response = TransactionMapper.map(input)
        response.contractAddress shouldBeEqualTo input.contractAddress
        response.privateKey shouldBeEqualTo input.privateKey
        response.receiverAddress shouldBeEqualTo input.receiverKey
    }

    @Test
    fun `map transaction to transaction payload test`() {
        val input = Transaction(contractAddress = "address", privateKey = "private", receiverKey = "receiver")
        val response = TransactionToTransactionPayloadMapper.map(input)
        response.contractAddress shouldBeEqualTo input.contractAddress
        response.privateKey shouldBeEqualTo input.privateKey
        response.receiverAddress shouldBeEqualTo input.receiverKey
    }

    @Test
    fun `Mapping WalletConfig to WalletPayload Test`() {
        val walletConfig = WalletConfig(
            0,
            identities,
            accounts
        )
        val walletPayload = WalletConfigToWalletPayloadMapper.map(walletConfig)

        walletConfig.version shouldBeEqualTo walletPayload.version
        walletConfig.identities[0].name shouldBeEqualTo walletPayload.identityResponse[0].name
        walletConfig.accounts[0].name shouldBeEqualTo walletPayload.accountResponse[0].name
    }

    @Test
    fun `map qr code result to credential qr code response`() {
        val result = mapHashMapToQrCodeResponse(credentialQrCodeResult, "token")
        assert(result is CredentialQrCode)
        (result as CredentialQrCode).run {
            memberName == "name" &&
                    name == "card" &&
                    issuer == "did:ethr:01016a194e4d5beee3a634edb156f84d03354a03" &&
                    token == "token" &&
                    memberId == "123456" &&
                    creationDate == "2018" &&
                    coverage == "touring" &&
                    iconUrl == "urlImage" &&
                    cardUrl == "urlCard"
        }
    }

    @Test
    fun `map qr code result to credential qr code response with no vc type`() {
        val result = mapHashMapToQrCodeResponse(credentialQrCodeResultWithDifferentVCType, "token")
        assert(result is CredentialQrCode)
        (result as CredentialQrCode).run {
            name == "" &&
                    memberId == "" &&
                    creationDate == "" &&
                    coverage == ""
        }
    }

    @Test
    fun `map qr code result to service qr code response`() {
        val result = mapHashMapToQrCodeResponse(serviceQrCodeResult, "token")
        assert(result is ServiceQrCode)
        (result as ServiceQrCode).run {
            serviceName == "Demo Web Page Login" &&
                    callback == "callback" &&
                    requestedData[0] == "test1" &&
                    identityFields == "test1 test2" &&
                    issuer == "did:ethr:0x95b200870916377a74fc65d628a735d58bc22c98" &&
                    token == "token" &&
                    identityFields == "touring"
        }
    }
}