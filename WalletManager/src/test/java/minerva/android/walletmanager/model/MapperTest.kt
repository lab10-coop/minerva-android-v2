package minerva.android.walletmanager.model

import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import org.amshove.kluent.shouldEqual
import org.junit.Test

class MapperTest : WalletConfigTestValues() {

    @Test
    fun `Mapping Identity to IdentityResponse Test`() {
        val identity = Identity(
            0,
            "publicKey",
            "privateKey",
            "IdentityTest",
            identityData,
            true
        )
        val identityResponse = mapIdentityToIdentityResponse(identity)

        identity.index shouldEqual identityResponse.index
        identity.name shouldEqual identityResponse.name
        identity.data shouldEqual identityResponse.data
        identity.isRemovable shouldEqual identityResponse.isRemovable
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

        identity.index shouldEqual identityResponse.index
        identity.name shouldEqual identityResponse.name
        identity.data shouldEqual identityResponse.data
        identity.isRemovable shouldEqual identityResponse.isRemovable
    }

    @Test
    fun `Mapping Value to ValueResponse Test`() {
        val value = Value(
            0,
            "publicKey",
            "privateKey",
            "ValueTest",
            "ValueNetworkTest"
        )

        val valueResponse = mapValueToValueResponse(value)

        value.index shouldEqual valueResponse.index
        value.name shouldEqual valueResponse.name
        value.network shouldEqual value.network
    }

    @Test
    fun `Mapping ValueResponse to Value Test`() {
        val valueResponse = ValuePayload(
            0,
            "ValueResponseTest",
            "ValueNetworkTest"
        )

        val value = mapValueResponseToValue(valueResponse)

        value.index shouldEqual valueResponse.index
        value.name shouldEqual valueResponse.name
        value.network shouldEqual value.network
    }

    @Test
    fun `Mapping WalletConfig to WalletPayload Test`() {
        val walletConfig = WalletConfig(
            0,
            identities,
            values
        )
        val walletPayload = mapWalletConfigToWalletPayload(walletConfig)

        walletConfig.version shouldEqual walletPayload.version
        walletConfig.identities[0].name shouldEqual walletPayload.identityResponses[0].name
        walletConfig.values[0].name shouldEqual walletPayload.valueResponses[0].name
    }

    @Test
    fun `Mapping WalletPayload to WalletConfig`() {
        val walletConfigPayload = WalletConfigPayload(
            0,
            identityResponses,
            valuesResponse
        )
        val walletConfigResponse = WalletConfigResponse(
            "testState",
            "testMessage",
            walletConfigPayload
        )

        val walletConfig = mapWalletConfigResponseToWalletConfig(walletConfigResponse)

        walletConfig.version shouldEqual walletConfigPayload.version
        walletConfig.identities[0].name shouldEqual walletConfigPayload.identityResponses[0].name
        walletConfig.values[0].name shouldEqual walletConfigPayload.valueResponses[0].name
    }

}