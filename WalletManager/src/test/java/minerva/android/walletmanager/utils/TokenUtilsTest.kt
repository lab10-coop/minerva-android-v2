package minerva.android.walletmanager.utils

import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class TokenUtilsTest {
    @Test
    fun `Check that generating key for map is correct`() {
        val chaiId = 3
        val address = "0x4ddr355"
        val key = generateTokenHash(chaiId, address)
        key shouldBeEqualTo "30x4ddr355"
    }
}