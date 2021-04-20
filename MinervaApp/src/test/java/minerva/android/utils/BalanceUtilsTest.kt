package minerva.android.utils

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.utils.BalanceUtils
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class BalanceUtilsTest {

    @Test
    fun `get crypto balance success test`() {
        val result1 = BalanceUtils.getCryptoBalance(BigDecimal.TEN)
        assertEquals(result1, "10")

        val result2 = BalanceUtils.getCryptoBalance(BigDecimal("0.00000000"))
        assertEquals(result2, "0")

        val result3 = BalanceUtils.getCryptoBalance(BigDecimal("0"))
        assertEquals(result3, "0")

        val result4 = BalanceUtils.getCryptoBalance(BigDecimal("0.04"))
        assertEquals(result4, "0.04")

        val result5 = BalanceUtils.getCryptoBalance(BigDecimal("3.1"))
        assertEquals(result5, "3.1")

        val result6 = BalanceUtils.getCryptoBalance(BigDecimal("3.00001"))
        assertEquals(result6, "3.00001")

        val result7 = BalanceUtils.getCryptoBalance(BigDecimal("3.343434678888"))
        assertEquals(result7, "3.343435")

        val result8 = BalanceUtils.getCryptoBalance(BigDecimal("0.0000004"))
        assertEquals(result8, "0.000001")

        val result9 = BalanceUtils.getCryptoBalance(BigDecimal("0.0000008"))
        assertEquals(result9, "0.000001")

        val result10 = BalanceUtils.getCryptoBalance(BigDecimal("0.0000000008"))
        assertEquals(result10, "0.000001")

        val result11 = BalanceUtils.getCryptoBalance(BigDecimal("0.0034000999"))
        assertEquals(result11, "0.003401")
    }

    @Test
    fun `get crypto balance error test`() {
        val result = BalanceUtils.getCryptoBalance(Int.InvalidValue.toBigDecimal())
        assertEquals(result, "0")
    }

    @Test
    fun `get fiat balance success test`() {
        val result = BalanceUtils.getFiatBalance(BigDecimal.TEN, "€")
        assertEquals(result, "€ 10.00")
    }

    @Test
    fun `get fiat balance error test`() {
        val result = BalanceUtils.getFiatBalance(Int.InvalidValue.toBigDecimal(), "€")
        assertEquals(result, "€ -.--")
    }

    @Test
    fun `Check converting from Wei` () {
        val balanceInWei = 10000000000000.toBigDecimal()
        val result = BalanceUtils.convertFromWei(balanceInWei, 10)
        result shouldBeEqualTo 1000.toBigDecimal()
    }
}