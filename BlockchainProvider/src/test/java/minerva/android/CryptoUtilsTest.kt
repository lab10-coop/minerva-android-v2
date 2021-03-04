package minerva.android

import minerva.android.blockchainprovider.utils.CryptoUtils
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class CryptoUtilsTest {

    @Test
    fun `token amount conversion test`() {
        val result = CryptoUtils.convertTokenAmount(BigDecimal(2), 6)
        assertEquals(result, BigInteger.valueOf(2000000))

        val result2 = CryptoUtils.convertTokenAmount(BigDecimal(2), 0)
        assertEquals(result2, BigInteger.valueOf(2))
    }

    @Test
    fun `prepare account name test`() {
        val result = CryptoUtils.prepareName("ETH", 1)
        assertEquals(result, "#2 ETH")
    }


    @Test
    fun `encode public key test`() {
        val result = CryptoUtils.encodePublicKey("01234/sad")
        assertEquals(result, "01234%2Fsad")
    }
}