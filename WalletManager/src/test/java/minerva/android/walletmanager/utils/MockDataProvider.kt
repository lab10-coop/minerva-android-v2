package minerva.android.walletmanager.utils

import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.network.SuperFluid
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.WalletConfigTestValues.accountsResponse
import minerva.android.walletmanager.model.token.WalletConfigTestValues.identityResponse
import minerva.android.walletmanager.model.token.WalletConfigTestValues.onlineIdentityResponse
import minerva.android.walletmanager.model.wallet.WalletConfig

object MockDataProvider {

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
            chainId = ATS_TAU, testNet = true,
            tokens = listOf(
                ERCToken(ATS_TAU, "CookieTokenDATS", "Cookie", "0xC00k1eN", "13", type = TokenType.ERC20),
                ERCToken(ATS_TAU, "SomeSomeTokenDATS", "SST", "0xS0m3T0k3N", "32", type = TokenType.ERC20),
                ERCToken(ATS_TAU, "CookieTokenDATS", "Cookie", "0xC00k1eN", "13", type = TokenType.ERC20),
                ERCToken(ATS_TAU, "SomeSomeTokenDATS", "SST", "0xS0m3T0k3N", "32", type = TokenType.ERC20)
            ),
            superfluid = SuperFluid("host", "netFlow")
        ),
        Network(
            chainId = ETH_RIN, testNet = true,
            tokens = listOf(
                ERCToken(ETH_RIN, "CookieTokenDETH", "Cookie", "0xC00k1e", "13", type = TokenType.ERC20),
                ERCToken(ETH_RIN, "OtherTokenDETH", "Cookie", "0x0th3rDD", "13", type = TokenType.ERC20)
            ),
            superfluid = SuperFluid("host", "netFlow")
        ),
        Network(chainId = ATS_SIGMA, testNet = true),
        Network(chainId = POA_CORE, testNet = true),
        Network(
            chainId = ETH_MAIN, testNet = false,
            tokens = listOf(
                ERCToken(ATS_TAU, "CookieTokenDATS", "Cookie", "0xC00k1eN", "13", type = TokenType.ERC20),
                ERCToken(ATS_TAU, "SomeSomeTokenDATS", "SST", "0xS0m3T0k3N", "32", type = TokenType.ERC20)
            )
        )
    )

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "11", "privateKey", "address1", data),
            Identity(1, "identityName2", "", "privateKey", "address2", data),
            Identity(3, "identityName3", "", "privateKey", "address3", data)
        ),
        listOf(
            Account(1, "publicKey1", "privateKey1", "address1", chainId = ETH_RIN, _isTestNetwork = true),
            Account(2, "publicKey2", "privateKey2", "address2", chainId = ATS_TAU, _isTestNetwork = true),
            Account(
                3, "publicKey3", "privateKey3", "address3", chainId = ATS_TAU,
                owners = listOf("masterOwner"), _isTestNetwork = true
            ),
            Account(
                4, "publicKey4", "privateKey4", "address4", chainId = ATS_TAU,
                owners = listOf("notMasterOwner", "masterOwner"), _isTestNetwork = true
            ),
            Account(5, "publicKey5", "privateKey5", "address4", chainId = ATS_TAU, _isTestNetwork = true),
            Account(1, "publicKey1Main", "privateKey1Main", "address1Main", chainId = ATS_SIGMA, _isTestNetwork = false),
            Account(2, "publicKey2Main", "privateKey2Main", "address2Main", chainId = POA_CORE, _isTestNetwork = false)
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
                    ERCToken(
                        ATS_TAU,
                        "CookieTokenATS",
                        "Cookie",
                        "0xC00k1eN",
                        "13",
                        accountAddress = "address4455",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "OtherTokenATS1",
                        "OtherC",
                        "0xS0m3T0k3N",
                        "32",
                        accountAddress = "address4455",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "OtherTokenATS",
                        "OtherC",
                        "0xC00k1e",
                        "32",
                        accountAddress = "address4455",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "TokenTest1",
                        "OtherC",
                        "0x0th3r",
                        "32",
                        accountAddress = "address4455",
                        type = TokenType.ERC20
                    )
//                    ERC20Token(ATS_TAU, "TokenTest1", "OtherC", "address1", "32", accountAddress = "address4455")
                )
            ),
            Pair(
                ETH_RIN, listOf(
                    ERCToken(
                        ETH_RIN,
                        "OtherTokenETH",
                        "OtherC",
                        "0x0th3r",
                        "32",
                        logoURI = "someLogoURI",
                        accountAddress = "address123",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ETH_RIN,
                        "CookieTokenETH",
                        "Cookie",
                        "0xC00k1e",
                        "13",
                        logoURI = "someLogoURI_II",
                        accountAddress = "address123",
                        type = TokenType.ERC20
                    )
                )
            ),
            Pair(
                ATS_SIGMA, listOf(
                    ERCToken(ATS_SIGMA, "CookieTokenATS", "Cookie", "0xC00k1e", "13", "0xADDRESSxTWO", type = TokenType.ERC20),
                    ERCToken(ATS_SIGMA, "SecondOtherATS", "Other22", "0x0th3r22", "22", "0xADDRESSxTWO", type = TokenType.ERC20),
                    ERCToken(ATS_SIGMA, "OtherTokenATS", "OtherC", "0x0th3r", "32", "0xADDRESSxTWO", type = TokenType.ERC20)
                )
            )
        )
    )
}