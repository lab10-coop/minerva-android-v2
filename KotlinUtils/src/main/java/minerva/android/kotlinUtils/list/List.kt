package minerva.android.kotlinUtils.list

fun <T> List<T>.inBounds(index: Int) = index in 0 until size

fun <T> MutableList<T>.removeAll(predicate: (T) -> Boolean): Boolean =
    mutableListOf<T>().let {
        forEach { element -> if (predicate(element)) it.add(element) }
        removeAll(it)
    }

fun <T> List<T>.mergeWithoutDuplicates(list: List<T>): List<T> =
    mutableListOf<T>().apply {
        addAll(this@mergeWithoutDuplicates)
        list.forEach {
            if (!contains(it)) add(it)
        }
    }
