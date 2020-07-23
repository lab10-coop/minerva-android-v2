package minerva.android.kotlinUtils

interface Mapper<T, R> {
    fun map(input: T): R
}