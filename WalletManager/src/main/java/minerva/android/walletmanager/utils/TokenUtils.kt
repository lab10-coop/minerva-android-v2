package minerva.android.walletmanager.utils

import java.util.*

//TODO klop add tests
object TokenUtils {
    fun generateTokenHash(chainId: Int, address: String) = "$chainId$address".toLowerCase(Locale.ROOT)
}