package minerva.android.walletmanager.mappers

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.WalletConfigTestValues
import minerva.android.walletmanager.model.mappers.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class MapperTest : WalletConfigTestValues() {

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
}