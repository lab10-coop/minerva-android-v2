package minerva.android.utils

internal val <T> T.exhaustive: T
    get() = this

val Any?.safe get() = Unit