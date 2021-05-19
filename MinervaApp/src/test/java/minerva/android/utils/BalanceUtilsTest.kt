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
        assertEquals("10", result1)

        val result2 = BalanceUtils.getCryptoBalance(0.00000000001.toBigDecimal())
        assertEquals("<0.0000000001", result2)

        val result3 = BalanceUtils.getCryptoBalance(BigDecimal.ZERO)
        assertEquals("0", result3)

        val result4 = BalanceUtils.getCryptoBalance(0.04.toBigDecimal())
        assertEquals("0.04", result4)

        val result5 = BalanceUtils.getCryptoBalance(3.1.toBigDecimal())
        assertEquals("3.1", result5)

        val result6 = BalanceUtils.getCryptoBalance(3.00001.toBigDecimal())
        assertEquals("3.00001", result6)

        val result7 = BalanceUtils.getCryptoBalance(3.343434678888.toBigDecimal())
        assertEquals("3.3434346789", result7)

        val result8 = BalanceUtils.getCryptoBalance(0.00000000004.toBigDecimal())
        assertEquals("<0.0000000001", result8)

        val result9 = BalanceUtils.getCryptoBalance(0.0000008.toBigDecimal())
        assertEquals("0.0000008", result9)

        val result10 = BalanceUtils.getCryptoBalance(0.0000000008.toBigDecimal())
        assertEquals("0.0000000008", result10)

        val result11 = BalanceUtils.getCryptoBalance(0.0034000999.toBigDecimal())
        assertEquals("0.0034000999", result11)

        val result12 = BalanceUtils.getCryptoBalance(BigDecimal("0.00000001"))
        assertEquals(result12, "0.00000001")

        val result13 = BalanceUtils.getCryptoBalance(BigDecimal("0.0001"))
        assertEquals(result13, "0.0001")

        val result14 = BalanceUtils.getCryptoBalance(BigDecimal("0.01"))
        assertEquals(result14, "0.01")

        val result15 = BalanceUtils.getCryptoBalance(BigDecimal("1"))
        assertEquals(result15, "1")

        val result16 = BalanceUtils.getCryptoBalance(BigDecimal("270"))
        assertEquals(result16, "270")
    }

    @Test
    fun `get crypto balance error test`() {
        val result = BalanceUtils.getCryptoBalance(Double.InvalidValue.toBigDecimal())
        assertEquals(result, "0")
    }

    @Test
    fun `get fiat balance success test`() {
        val result = BalanceUtils.getFiatBalance(BigDecimal.TEN, "€")
        assertEquals(result, "€ 10.00")
    }

    @Test
    fun `get fiat balance error test`() {
        val result = BalanceUtils.getFiatBalance(Double.InvalidValue.toBigDecimal(), "€")
        assertEquals(result, "€ -.--")
    }

    @Test
    fun `Check converting from Wei`() {
        val balanceInWei = 10000000000000.toBigDecimal()
        val result = BalanceUtils.convertFromWei(balanceInWei, 10)
        result shouldBeEqualTo 1000.toBigDecimal()
    }
}