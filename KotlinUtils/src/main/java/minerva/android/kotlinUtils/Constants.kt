package minerva.android.kotlinUtils

val String.Companion.Empty: String
    get() = ""

val String.Companion.Space: String
    get() = " "

val String.Companion.NO_DATA: String
    get() = ""

val String.Companion.EmptyBalance: String
    get() = "0"

val Int.Companion.InvalidId: Int
    get() = -1

val Int.Companion.InvalidIndex: Int
    get() = -1

val Int.Companion.InvalidVersion: Int
    get() = -1

val Int.Companion.InvalidValue: Int
    get() = -1
