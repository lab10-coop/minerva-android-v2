package minerva.android.kotlinUtils.list

fun <T> List<T>.inBounds(index: Int) = this.size > index && index >= 0