package minerva.android.walletmanager.storage

import minerva.android.kotlinUtils.InvalidValue
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RateStorageTest {

    @Test
    fun `checking temp storage`() {
        val tempStorage = RateStorageImpl()
        tempStorage.getRate("missing_hash") shouldBeEqualTo Double.InvalidValue
        tempStorage.getRates().size shouldBeEqualTo 0
        tempStorage.saveRate("hash", 3.3)
        tempStorage.getRates().size shouldBeEqualTo 1
        tempStorage.getRate("hash") shouldBeEqualTo 3.3
    }
}