package minerva.android.kotlinUtils.mapper

object StringArrayMapper {

    private const val VALUE_LIMIT = 2
    private const val MAP_STRING_SEPARATOR = "|"
    private const val KEY_INDEX = 0
    private const val VALUE_INDEX = 1

    fun mapStringArray(array: Array<String>): Map<String, String> =
            mutableMapOf<String, String>().apply {
                array.forEach {
                    it.split(MAP_STRING_SEPARATOR, limit = VALUE_LIMIT).let { splitResult ->
                        put(splitResult[KEY_INDEX], splitResult[VALUE_INDEX])
                    }
                }
        }
}