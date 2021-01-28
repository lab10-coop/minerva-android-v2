package minerva.android.walletmanager.utils

import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.WalletConfigTestValues.accountsResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.identityResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.onlineIdentityResponse
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.token.ERC20Token

object DataProvider {

    val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    val localWalletConfigPayload =
        WalletConfigPayload(_version = 1, _identityPayloads = identityResponse, _accountPayloads = accountsResponse)

    val onlineWalletConfigResponse =
        WalletConfigPayload(_version = 0, _identityPayloads = onlineIdentityResponse, _accountPayloads = accountsResponse)

    val networks = listOf(
        Network(
            short = NetworkShortName.ATS_TAU, httpRpc = "address", testNet = true,
            tokens = listOf(
                ERC20Token(3, "CookieTokenDATS", "Cookie", "0xC00k1eN", "13"),
                ERC20Token(3, "SomeSomeTokenDATS", "SST", "0xS0m3T0k3N", "32")
            )
        ),
        Network(
            short = NetworkShortName.ETH_RIN, httpRpc = "address", testNet = true,
            tokens = listOf(
                ERC20Token(3, "CookieTokenDETH", "Cookie", "0xC00k1e", "13"),
                ERC20Token(3, "OtherTokenDETH", "Cookie", "0x0th3rDD", "13")
            )
        ),
        Network(short = NetworkShortName.ATS_SIGMA, httpRpc = "address", testNet = true),
        Network(short = NetworkShortName.POA_CORE, httpRpc = "address", testNet = true)
    )

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "11", "privateKey", "address1", data),
            Identity(1, "identityName2", "", "privateKey", "address2", data),
            Identity(3, "identityName3", "", "privateKey", "address3", data)
        ),
        listOf(
            Account(2, "publicKey1", "privateKey1", "address", networkShort = NetworkShortName.ETH_RIN),
            Account(4, "publicKey2", "privateKey2", "address", networkShort = NetworkShortName.ATS_TAU),
            Account(
                5, "publicKey3", "privateKey3", "address", networkShort = NetworkShortName.ATS_TAU,
                owners = listOf("masterOwner")
            ),
            Account(
                6, "publicKey4", "privateKey4", "address", networkShort = NetworkShortName.ATS_TAU,
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Account(7, "publicKey5", "privateKey5", "address", networkShort = NetworkShortName.ATS_TAU),
            Account(
                1,
                "publicKey1Main",
                "privateKey1Main",
                "address1Main",
                networkShort = NetworkShortName.ATS_SIGMA
            ),
            Account(
                2,
                "publicKey2Main",
                "privateKey2Main",
                "address2Main",
                networkShort = NetworkShortName.POA_CORE
            )
        ),
        listOf(
            Service("1", "name")
        ),
        listOf(
            Credential(
                loggedInIdentityDid = "did:ethr:address",
                type = CredentialType.VERIFIABLE_CREDENTIAL.type,
                membershipType = CredentialType.AUTOMOTIVE_CLUB.type,
                issuer = "iss"
            )
        ),
        mapOf(
            Pair(
                NetworkShortName.ATS_TAU, listOf(
                    ERC20Token(3, "CookieTokenATS", "Cookie", "0xC00k1e", "13"),
                    ERC20Token(3, "OtherTokenATS", "OtherC", "0x0th3r", "32")
                )
            ),
            Pair(NetworkShortName.ETH_RIN, listOf(
                ERC20Token(3, "OtherTokenETH", "OtherC", "0x0th3r", "32"),
                ERC20Token(3, "CookieTokenETH", "Cookie", "0xC00k1e", "13")
            ))
        )
    )
}