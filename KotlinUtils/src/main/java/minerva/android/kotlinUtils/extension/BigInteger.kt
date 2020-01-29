package minerva.android.kotlinUtils.extension

import java.math.BigInteger

fun BigInteger.toHexString() = "0x" + toString(16)