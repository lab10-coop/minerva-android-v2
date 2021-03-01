package minerva.android.walletmanager.utils

import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.WalletConfigTestValues.accountsResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.identityResponse
import minerva.android.walletmanager.model.WalletConfigTestValues.onlineIdentityResponse
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.wallet.WalletConfig

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
            chainId = ATS_TAU, httpRpc = "address", testNet = true,
            tokens = listOf(
                ERC20Token(1, "CookieTokenDATS", "Cookie", "0xC00k1eN", "13"),
                ERC20Token(1, "SomeSomeTokenDATS", "SST", "0xS0m3T0k3N", "32")
            )
        ),
        Network(
            chainId = ETH_RIN, httpRpc = "address", testNet = true,
            tokens = listOf(
                ERC20Token(2, "CookieTokenDETH", "Cookie", "0xC00k1e", "13"),
                ERC20Token(2, "OtherTokenDETH", "Cookie", "0x0th3rDD", "13")
            )
        ),
        Network(chainId = ATS_SIGMA, httpRpc = "address", testNet = true),
        Network(chainId = POA_CORE, httpRpc = "address", testNet = true)
    )

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "11", "privateKey", "address1", data),
            Identity(1, "identityName2", "", "privateKey", "address2", data),
            Identity(3, "identityName3", "", "privateKey", "address3", data)
        ),
        listOf(
            Account(1, "publicKey1", "privateKey1", "address1", chainId = ETH_RIN),
            Account(2, "publicKey2", "privateKey2", "address2", chainId = ATS_TAU),
            Account(
                3, "publicKey3", "privateKey3", "address3", chainId = ATS_TAU,
                owners = listOf("masterOwner")
            ),
            Account(
                4, "publicKey4", "privateKey4", "address4", chainId = ATS_TAU,
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Account(1, "publicKey5", "privateKey5", "address4", chainId = ATS_TAU),
            Account(5, "publicKey1Main", "privateKey1Main", "address1Main", chainId = ATS_SIGMA),
            Account(6, "publicKey2Main", "privateKey2Main", "address2Main", chainId = POA_CORE)
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
                ATS_TAU, listOf(
                    ERC20Token(ATS_TAU, "CookieTokenATS", "Cookie", "0xC00k1e", "13"),
                    ERC20Token(ATS_TAU, "OtherTokenATS", "OtherC", "0x0th3r", "32")
                )
            ),
            Pair(
                ETH_RIN, listOf(
                    ERC20Token(ETH_RIN, "OtherTokenETH", "OtherC", "0x0th3r", "32", "someLogoURI"),
                    ERC20Token(ETH_RIN, "CookieTokenETH", "Cookie", "0xC00k1e", "13", "someLogoURI_II")
                )
            ),
            Pair(
                ATS_SIGMA, listOf(
                    ERC20Token(ATS_SIGMA, "CookieTokenATS", "Cookie", "0xC00k1e", "13"),
                    ERC20Token(ATS_SIGMA, "SecondOtherATS", "Other22", "0x0th3r22", "22"),
                    ERC20Token(ATS_SIGMA, "OtherTokenATS", "OtherC", "0x0th3r", "32")
                )
            )
        )
    )
}