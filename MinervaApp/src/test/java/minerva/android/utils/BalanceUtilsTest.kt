package minerva.android.utils

import minerva.android.kotlinUtils.InvalidValue
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class BalanceUtilsTest {

    @Test
    fun `get crypto balance success test`(){
        val result = BalanceUtils.getCryptoBalance(BigDecimal.TEN)
        assertEquals(result, "10")
    }

    @Test
    fun `get crypto balance error test`(){
        val result = BalanceUtils.getCryptoBalance(Int.InvalidValue.toBigDecimal())
        assertEquals(result, "-.--")
    }

    @Test
    fun`get fiat balance success test`(){
        val result = BalanceUtils.getFiatBalance(BigDecimal.TEN)
        assertEquals(result, "€ 10.00")
    }

    @Test
    fun`get fiat balance error test`(){
        val result = BalanceUtils.getFiatBalance(Int.InvalidValue.toBigDecimal())
        assertEquals(result, "")
    }
}