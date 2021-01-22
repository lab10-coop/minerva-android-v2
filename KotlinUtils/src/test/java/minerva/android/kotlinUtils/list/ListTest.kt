package minerva.android.kotlinUtils.list

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ListTest {

    private val startList = listOf("Some", "Some1", "Some2", "Some3", "Some3", "Some1", "Some3")

    private val mergeListOne = listOf("Some1", "Some2", "Some3", "Some4")
    private val mergeListTwo = listOf("Some4", "Some5", "Some6", "Some7")

    private val startMutableList
        get() = startList.toMutableList()

    @Test
    fun `Check inBounds extension` () {
        startList.apply {
            inBounds(-1) shouldBeEqualTo false
            inBounds(0) shouldBeEqualTo true
            inBounds(3) shouldBeEqualTo true
            inBounds(6) shouldBeEqualTo true
            inBounds(7) shouldBeEqualTo false
        }
    }

    @Test
    fun `Check removingAll extension`() {
        var wasRemoving: Boolean
        startMutableList.apply {
            wasRemoving = removeAll { it == "Some3" }
            size shouldBeEqualTo 4
            wasRemoving shouldBeEqualTo true
            wasRemoving = removeAll { it == "Cookie" }
            size shouldBeEqualTo 4
            wasRemoving shouldBeEqualTo false
            wasRemoving = removeAll { it == "Some" }
            size shouldBeEqualTo 3
            wasRemoving shouldBeEqualTo true
        }
    }

    @Test
    fun `Check merging two lists without duplications` () {
        mergeListOne.mergeWithoutDuplicates(mergeListTwo).size shouldBeEqualTo 7
    }
}