package minerva.android.kotlinUtils.mapper

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class StringArrayMapperTest {

    @Test
    fun `Check correct mapping string array to map`() {
        val testArray = arrayOf(
            "key01|value01",
            "key02|value02",
            "key03|value03",
            "key04|value04",
            "key05|value05",
            "key06|value06",
            "key07|value07"
        )
        val result = StringArrayMapper.mapStringArray(testArray)

        result.size shouldBeEqualTo 7
    }
}