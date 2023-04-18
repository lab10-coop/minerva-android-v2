package minerva.android.kotlinUtils

import java.math.BigDecimal

val String.Companion.ONE: String
    get() = "1"

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

val Int.Companion.NO_MARGIN: Int
    get() = 0

val Int.Companion.NO_PADDING: Int
    get() = 0

val Int.Companion.FirstIndex: Int
    get() = 0

val Int.Companion.ZERO: Int
    get() = 0

val Int.Companion.ONE: Int
    get() = 1

val Int.Companion.TWO: Int
    get() = 2

val Int.Companion.THREE: Int
    get() = 3

val Int.Companion.EIGHT: Int
    get() = 8

val Int.Companion.OneElement: Int
    get() = 1

val Int.Companion.InvalidIndex: Int
    get() = -1

val Int.Companion.InvalidValue: Int
    get() = -1

val Int.Companion.EmptyResource: Int
    get() = 0

val Long.Companion.InvalidValue: Long
    get() = -1L

val Double.Companion.InvalidValue: Double
    get() = -1.0

val Double.Companion.InvalidBigDecimal: BigDecimal
    get() = (-1.0).toBigDecimal()