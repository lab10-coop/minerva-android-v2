package minerva.android.token

import com.nhaarman.mockitokotlin2.*
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.ONE
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal

class ManageTokensViewModelTest : BaseViewModelTest() {
    private val accountManager: AccountManager = mock()
    private val localStorage: LocalStorage = mock()
    private val tokenManager: TokenManager = mock()
    private val viewModel =
        ManageTokensViewModel(accountManager, localStorage, tokenManager)

    private val networks = listOf(
        Network(chainId = 1, httpRpc = "address", testNet = true),
        Network(chainId = 2, httpRpc = "address", testNet = true),
        Network(chainId = 3, httpRpc = "address", testNet = true, token = "cookie")
    )

    private val account = Account(
        id = 0,
        publicKey = "12",
        privateKey = "12",
        chainId = 3,
        address = "address",
        contractAddress = "aa",
        bindedOwner = "binded",
        accountTokens = mutableListOf(
            AccountToken(ERCToken(1, symbol = "token1", address = "address1", type = TokenType.ERC20), BigDecimal.ONE),
            AccountToken(ERCToken(3, symbol = "token2", address = "address2", type = TokenType.ERC20), BigDecimal.ONE),
            AccountToken(ERCToken(3, symbol = "token3", address = "address3", type = TokenType.ERC721), BigDecimal.ONE),
            AccountToken(ERCToken(3, symbol = "token3", address = "address3", type = TokenType.ERC721), BigDecimal.ONE),
            AccountToken(ERCToken(3, symbol = "token4", address = "address4", type = TokenType.ERC20), BigDecimal.ZERO),
            AccountToken(ERCToken(3, symbol = "token5", address = "address5", type = TokenType.ERC20), BigDecimal.ONE)
        )
    )

    private val ercTokenList: List<ERCToken> = account.accountTokens.map { it.token }

    @Test
    fun `Check loading correct tokens list for account`() {
        NetworkManager.initialize(networks)
        whenever(accountManager.loadAccount(any())).thenReturn(account)
        whenever(localStorage.getTokenVisibilitySettings()).thenReturn(TokenVisibilitySettings())
        whenever(tokenManager.getActiveTokensPerAccount(any())).thenReturn(ercTokenList)
        viewModel.initViewModel(0)

        val tokens = viewModel.loadTokens()
        tokens.size shouldBeEqualTo 6
        tokens[0].symbol shouldBeEqualTo "cookie"
        tokens[1].symbol shouldBeEqualTo "token1"
        tokens[2].symbol shouldBeEqualTo "token2"
        tokens[3].symbol shouldBeEqualTo "token4"
        tokens[4].symbol shouldBeEqualTo "token5"
    }

    @Test
    fun `Check that calling method getTokenVisibility is working when call this method`() {
        viewModel.tokenVisibilitySettings = mock()
        viewModel.account = Account(0)
        whenever(
            viewModel.tokenVisibilitySettings.getTokenVisibility(
                any(),
                any()
            )
        ).thenReturn(null, false, true)
        val resultOne = viewModel.getTokenVisibilitySettings("")
        val resultTwo = viewModel.getTokenVisibilitySettings("")
        val resultThree = viewModel.getTokenVisibilitySettings("")

        resultOne shouldBeEqualTo false
        resultTwo shouldBeEqualTo false
        resultThree shouldBeEqualTo true
    }

    @Test
    fun `Check that saving method was called`() {
        viewModel.account = Account(0)
        viewModel.tokenVisibilitySettings = mock()
        whenever(localStorage.saveTokenVisibilitySettings(any())).thenReturn(TokenVisibilitySettings())
        whenever(
            viewModel.tokenVisibilitySettings.updateTokenVisibility(
                any(),
                any(),
                any()
            )
        ).thenReturn(
            TokenVisibilitySettings()
        )
        viewModel.saveTokenVisibilitySettings("", false)

        verify(localStorage, times(1)).saveTokenVisibilitySettings(any())
    }
}