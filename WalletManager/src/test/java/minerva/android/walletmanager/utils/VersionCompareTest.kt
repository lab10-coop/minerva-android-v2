package minerva.android.walletmanager.utils

import org.junit.Test
import kotlin.test.assertEquals

class VersionCompareTest {

    @Test
    fun `compare version test`() {
        assertEquals(true, isNewVersionBigger("0.0.0", "0.0.1"))
        assertEquals(true, isNewVersionBigger("2.0.0", "3.0.1"))
        assertEquals(true, isNewVersionBigger("0.11.0", "0.12.1"))
        assertEquals(true, isNewVersionBigger("0.13.0", "1.0.1"))
        assertEquals(false, isNewVersionBigger("1.13.0", "1.0.1"))
        assertEquals(false, isNewVersionBigger("1.13.0", "1.13.0"))
        assertEquals(false, isNewVersionBigger("1.3.0", "1.0.2"))
        assertEquals(false, isNewVersionBigger("0.13.013", "0.13.0"))
        assertEquals(true, isNewVersionBigger(null, "0.13.0"))
    }
}