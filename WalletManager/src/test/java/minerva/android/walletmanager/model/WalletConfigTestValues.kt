package minerva.android.walletmanager.model

import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token

object WalletConfigTestValues {

    val identityData: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991",
        "Some Key" to "Some value",
        "Some Key 2" to "Some value",
        "Some Key 3" to "Some value",
        "Some Key 4" to "Some value"
    )

    val identity = listOf(
        Identity(0, "IdentityName1", "publicKey", "privateKey", "address", identityData, false)
    )

    val onlineIdentity = listOf(
        Identity(0, "OnlineIdentityName1", "publicKey", "privateKey", "address", identityData, false)
    )

    val identityResponse = listOf(
        IdentityPayload(0, "IdentityName1", identityData, false)
    )

    val onlineIdentityResponse = listOf(
        IdentityPayload(0, "OnlineIdentityName1", identityData, false)
    )

    val identities = listOf(
        Identity(2, "pubicKey", "privateKey", "IdentityName1", "address", identityData, false),
        Identity(3, "pubicKey", "privateKey", "IdentityName2", "address", identityData, true)
    )

    val tokens =
        mapOf(
            Pair(
                "user01", listOf(
                    ERC20Token("CookieCoin", "COOKiE", "0xC00k13", "13"),
                    ERC20Token("otherCoin", "OC", "0x0th3rC01n", "32")
                )
            ),
            Pair(
                "user02", listOf(
                    ERC20Token("diffCoin", "DiFF", "0xD1FF", "13")
                )
            )
        )


    val networks = listOf(
        Network(short = "artis_tau1", httpRpc = "httpRpc"),
        Network(short = "eth_rinkeby", httpRpc = "httpRpc")
    )

    val accounts = listOf(
        Account(1, "publicKey", "privateKey", "address", "ValuePayload1", networks[0].short, contractAddress = "test"),
        Account(2, "publicKey", "privateKey", "address", "ValuePayload2", networks[1].short)
    )

    val accountsResponse = listOf(
        AccountPayload(1, "ValuePayload1", networks[0].short, false, _contractAddress = "contractAddress"),
        AccountPayload(2, "ValuePayload2", networks[1].short, false, _contractAddress = "contractAddress")
    )
}
