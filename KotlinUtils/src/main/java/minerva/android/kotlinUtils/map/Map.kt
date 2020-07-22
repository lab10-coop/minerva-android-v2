package minerva.android.kotlinUtils.map

fun <T, K> Map<T, K>.value(position: T): K = this[position] ?: error("Value not found")