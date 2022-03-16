package minerva.android.utils

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HexColorUtilsTest {

    @Test
    fun `hex color WITHOUT hex sign after ensured hex color prefix called contains only one hex sign`() {
        val colors = listOf("123456", "111111", "0Ef341")

        colors
            .map { ensureHexColorPrefix(it) }
            .onEach { assertEquals('#', it[0]) }
            .forEach { assertNotEquals('#', it[1]) }
    }

    @Test
    fun `hex color WITH hex sign after ensured hex color prefix called contains only one hex sign`() {
        val colors = listOf("#123456", "#111111", "#0Ef341")

        colors
            .map { ensureHexColorPrefix(it) }
            .onEach { assertEquals('#', it[0]) }
            .forEach { assertNotEquals('#', it[1]) }
    }
}