package minerva.android.walletmanager.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class BalanceUtilsTest {

    @Test
    fun `Check converting from Wei` () {
        val balanceInWei = 10000000000000.toBigDecimal()
        val result = BalanceUtils.fromWei(balanceInWei, 10)
        result shouldBeEqualTo 1000.toBigDecimal()
    }
}