package minerva.android.units

import minerva.android.blockchainprovider.repository.units.UnitConverterImpl
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class UnitConverterTest {

    private val converter = UnitConverterImpl()

    @Test
    fun `to ether conversion test`() {
        val result = converter.toEther(BigDecimal.valueOf(1000000000000000000))
        assertEquals(result, BigDecimal.ONE)

        val result2 = converter.toEther(BigDecimal("1"))
        assertEquals(result2, BigDecimal("1E-18"))
    }

    @Test
    fun `to gwei conversion test`() {
        val result = converter.toGwei(BigDecimal.ONE)
        assertEquals(result, BigDecimal.valueOf(1000000000))
    }
}