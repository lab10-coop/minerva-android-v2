package minerva.android.walletmanager.model

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ValuePayload
import minerva.android.walletmanager.model.mappers.*
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
        val identityResponse = mapIdentityToIdentityPayload(identity)

        identity.index shouldEqual identityResponse.index
        identity.name shouldEqual identityResponse.name
        identity.data shouldEqual identityResponse.data
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

        val valueResponse = mapValueToValuePayload(value)

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
        walletConfig.identities[0].name shouldEqual walletPayload.identityResponse[0].name
        walletConfig.values[0].name shouldEqual walletPayload.valueResponse[0].name
    }
}