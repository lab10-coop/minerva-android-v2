package minerva.android.walletmanager.model.token

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ERCTokenTest {

    @Test
    fun `Check equals method for ERC20Token`() {
        (tokenOne == tokenOne) shouldBeEqualTo true
        (tokenOne == tokenOneOne) shouldBeEqualTo true
        (tokenOne == tokenTwo) shouldBeEqualTo false
    }


    private val tokenOne =
        ERCToken(
            1,
            "nameOne",
            "symbolOne",
            "addressOne",
            "decimalsOne",
            "logoUriOne",
            type = TokenType.ERC20
        )

    private val tokenOneOne =
        ERCToken(
            1,
            "nameOne",
            "symbolOne",
            "ADDRESSoNE",
            "decimalsOne",
            "logoUriOne",
            type = TokenType.ERC20
        )

    private val tokenTwo =
        ERCToken(
            2,
            "nameTwo",
            "symbolTwo",
            "addressTwo",
            "decimalsTwo",
            "logoUriTwo",
            type = TokenType.ERC20
        )
}