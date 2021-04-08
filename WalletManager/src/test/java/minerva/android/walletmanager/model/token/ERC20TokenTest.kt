package minerva.android.walletmanager.model.token

import minerva.android.walletmanager.model.token.ERC20Token
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ERC20TokenTest {

    @Test
    fun `Check equals method for ERC20Token`() {
        (tokenOne == tokenOne) shouldBeEqualTo true
        (tokenOne == tokenOneOne) shouldBeEqualTo true
        (tokenOne == tokenTwo) shouldBeEqualTo false
    }


    private val tokenOne =
        ERC20Token(
            1,
            "nameOne",
            "symbolOne",
            "addressOne",
            "decimalsOne",
            "logoUriOne"
        )

    private val tokenOneOne =
        ERC20Token(
            1,
            "nameOne",
            "symbolOne",
            "ADDRESSoNE",
            "decimalsOne",
            "logoUriOne"
        )

    private val tokenTwo =
        ERC20Token(
            2,
            "nameTwo",
            "symbolTwo",
            "addressTwo",
            "decimalsTwo",
            "logoUriTwo"
        )
}