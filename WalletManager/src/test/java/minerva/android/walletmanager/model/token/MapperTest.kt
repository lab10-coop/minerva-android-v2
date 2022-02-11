package minerva.android.walletmanager.model.token

import minerva.android.apiProvider.model.TokenData
import minerva.android.apiProvider.model.TokensOwnedPayload
import minerva.android.apiProvider.model.TransactionSpeed
import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.configProvider.model.walletConfig.CredentialsPayload
import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.networks.NetworkManager.networks
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.token.WalletConfigTestValues.accounts
import minerva.android.walletmanager.model.token.WalletConfigTestValues.accountsResponse
import minerva.android.walletmanager.model.token.WalletConfigTestValues.identities
import minerva.android.walletmanager.model.token.WalletConfigTestValues.identityData
import minerva.android.walletmanager.model.token.WalletConfigTestValues.tokens
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.utils.MockDataProvider
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class MapperTest {

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
        val identity = IdentityPayloadToIdentityMapper.map(identityResponse)

        identity.index shouldBeEqualTo identityResponse.index
        identity.name shouldBeEqualTo identityResponse.name
        identity.personalData shouldBeEqualTo identityResponse.data
    }

    @Test
    fun `Mapping Account to AccountResponse Test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val value = Account(
            0,
            "publicKey",
            "privateKey",
            "ValueTest",
            "ValueNetworkTest",
            246785
        )

        val valueResponse = AccountToAccountPayloadMapper.map(value)

        value.id shouldBeEqualTo valueResponse.index
        value.name shouldBeEqualTo valueResponse.name
        value.network shouldBeEqualTo value.network
    }

    @Test
    fun `Mapping ERC20Token to ERC20TokenPayload Test`() {
        val value = ERCToken(3, "name", "symbol", "address", "decimals", "key", type = TokenType.ERC20)
        val valueResponse = ERCTokenToERC20TokenPayloadMapper.map(value)

        value.name shouldBeEqualTo valueResponse.name
        value.symbol shouldBeEqualTo valueResponse.symbol
        value.address shouldBeEqualTo valueResponse.address
        value.decimals shouldBeEqualTo valueResponse.decimals
        value.logoURI shouldBeEqualTo valueResponse.logoURI
        value.accountAddress shouldBeEqualTo "key"
    }

    @Test
    fun `Mapping ERC20TokenPayload to ERC20Token Test`() {
        val value = ERC20TokenPayload(1, "name", "symbol", "address", "decimals")
        val valueResponse = ERC20TokenPayloadToERCTokenMapper.map(value)

        valueResponse.name shouldBeEqualTo value.name
        valueResponse.symbol shouldBeEqualTo value.symbol
        valueResponse.address shouldBeEqualTo value.address
        valueResponse.decimals shouldBeEqualTo value.decimals
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
            val value = AccountPayloadToAccountMapper.map(it)

            value.id shouldBeEqualTo it.index
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
        val response = TransactionCostPayloadToTransactionCost.map(input, TransactionSpeed(), 1) { it }
        response.gasLimit shouldBeEqualTo input.gasLimit
        response.gasPrice shouldBeEqualTo input.gasPrice
        response.cost shouldBeEqualTo input.cost
        response.txSpeeds[0].value shouldBeEqualTo BigDecimal.ZERO
    }

    @Test
    fun `map transaction cost payload to transaction cost test when gas prices is null`() {
        val input = TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)
        val response = TransactionCostPayloadToTransactionCost.map(input, null, 1) { it }
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
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            0,
            identities,
            accounts,
            erc20Tokens = tokens
        )
        val walletPayload = WalletConfigToWalletPayloadMapper.map(walletConfig)

        walletConfig.version shouldBeEqualTo walletPayload.version
        walletConfig.identities[0].name shouldBeEqualTo walletPayload.identityResponse[0].name
        walletConfig.accounts[0].name shouldBeEqualTo walletPayload.accountResponse[0].name
        walletConfig.erc20Tokens[1]?.get(0)?.name shouldBeEqualTo walletPayload.erc20TokenResponse[1]?.get(0)?.name
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

    @Test
    fun `map TokenBalance to AccountToken`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokenBalance01 = TokenData(
            "type",
            "symbol",
            "Cookie Token",
            "10",
            "0xC00KiE01",
            "10000000000000"
        )
        val tokenBalance02 = TokenData(
            "type",
            "symbol",
            "Cookie Token 2",
            "18",
            "0xC00KiE02",
            "200000000000000000"
        )
        val tokenBalance03 = TokenData(
            "ERC-20"
        )
        val tokenBalance04 = TokenData(
            "ERC-721"
        )
        val tokenBalance05 = TokenData(
            "ERC-1155"
        )
        val result01 = TokenDataToERCToken.map(ATS_TAU, tokenBalance01, "key")
        result01.name shouldBeEqualTo "Cookie Token"
        result01.address shouldBeEqualTo "0xC00KiE01"
        val result02 = TokenDataToERCToken.map(ATS_TAU, tokenBalance02, "key")
        result02.name shouldBeEqualTo "Cookie Token 2"
        result02.address shouldBeEqualTo "0xC00KiE02"
        val result03 = TokenDataToERCToken.map(ATS_TAU, tokenBalance03, "key")
        result03.type.isERC20() shouldBeEqualTo true
        val result04 = TokenDataToERCToken.map(ATS_TAU, tokenBalance04, "key")
        result04.type.isERC721() shouldBeEqualTo true
        val result05 = TokenDataToERCToken.map(ATS_TAU, tokenBalance05, "key")
        result05.type.isERC1155() shouldBeEqualTo true

    }

    @Test
    fun `map TokensOwned to ERCToken`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokenOwned01 = TokensOwnedPayload.TokenOwned(
            "1",
            "address01",
            "18",
            emptyList(),
            "88",
            "Name",
            "Symbol",
            "uri",
            listOf("ERC-1155"),
            tokenJson = TokensOwnedPayload.TokenOwned.TokenJson(null, null, null, "imageUri", null, "animURi")
        )
        val tokenOwned02 = TokensOwnedPayload.TokenOwned(
            "10",
            "address02",
            "18",
            emptyList(),
            "88",
            "n4m8",
            "Symbol",
            "uri",
            listOf("ERC-721"),
            tokenJson = TokensOwnedPayload.TokenOwned.TokenJson(null, null, null, "imageUri", null, "animURi")
        )
        val tokenOwned03 = TokensOwnedPayload.TokenOwned(
            "1000",
            "address03",
            "18",
            emptyList(),
            "88",
            "Nam3",
            "Symb0l",
            "uri",
            listOf("ERC-20"),
            tokenJson = TokensOwnedPayload.TokenOwned.TokenJson(null, null, null, null, null, null)
        )

        val result01 = TokensOwnedToERCToken.map(ATS_TAU, tokenOwned01, "key")
        result01.name shouldBeEqualTo String.Empty
        result01.collectionName shouldBeEqualTo "Name"
        result01.address shouldBeEqualTo "address01"
        result01.type shouldBeEqualTo TokenType.ERC1155
        result01.nftContent.imageUri shouldBeEqualTo "imageUri"
        result01.nftContent.animationUri shouldBeEqualTo "animURi"
        val result02 = TokensOwnedToERCToken.map(ATS_TAU, tokenOwned02, "key")
        result02.collectionName shouldBeEqualTo "n4m8"
        result02.name shouldBeEqualTo String.Empty
        result02.address shouldBeEqualTo "address02"
        result02.type shouldBeEqualTo TokenType.ERC721
        result02.nftContent.imageUri shouldBeEqualTo "imageUri"
        result02.nftContent.animationUri shouldBeEqualTo "animURi"
        val result03 = TokensOwnedToERCToken.map(ATS_TAU, tokenOwned03, "key")
        result03.name shouldBeEqualTo "Nam3"
        result03.collectionName shouldBeEqualTo null
        result03.address shouldBeEqualTo "address03"
        result03.type shouldBeEqualTo TokenType.ERC20
        result03.nftContent.imageUri shouldBeEqualTo ""
        result03.nftContent.animationUri shouldBeEqualTo ""

    }

    @Test
    fun `map tx cost payload to tx cost data test`() {
        val result = TxCostPayloadToTxCostDataMapper.map(
            TxCostPayload(
                TransferType.COIN_TRANSFER,
                from = "address1",
                to = "address2"
            )
        )
        result.transferType shouldBeEqualTo BlockchainTransactionType.COIN_TRANSFER
        result.from shouldBeEqualTo "address1"
        result.to shouldBeEqualTo "address2"
    }

    @Test
    fun `map token with balance to coin crypto balance test`() {
        val input = TokenWithBalance(1, "aa", BigDecimal.TEN)
        val result = TokenToCoinCryptoBalanceMapper.map(input)
        result.address shouldBeEqualTo "aa"
        result.chainId shouldBeEqualTo 1
        result.balance shouldBeEqualTo BigDecimal.TEN
    }

    @Test
    fun `map token with balance error to coin crypto balance error test`() {
        val input = TokenWithError(1, "aa", Throwable("Balance Error"))
        val result = TokenToCoinBalanceErrorMapper.map(input)
        result.address shouldBeEqualTo "aa"
        result.chainId shouldBeEqualTo 1
        result.error.message shouldBeEqualTo "Balance Error"
    }
}