package minerva.android.walletmanager.model.token

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal

class AccountTokenTest {

    @Test
    fun `check getting correct values test` () {
        val erc20Token = ERCToken(1, "token", decimals = "5", type = TokenType.ERC20)
        val accountToken01 = AccountToken(erc20Token, 200000.toBigDecimal(), 3.3)
        val accountToken02 = AccountToken(erc20Token, BigDecimal.ZERO, 3.14)

        accountToken01.currentBalance shouldBeEqualTo 2.toBigDecimal()
        accountToken01.fiatBalance shouldBeEqualTo 6.6.toBigDecimal()
        accountToken02.currentBalance shouldBeEqualTo BigDecimal.ZERO
        accountToken02.fiatBalance shouldBeEqualTo BigDecimal.ZERO
    }
}