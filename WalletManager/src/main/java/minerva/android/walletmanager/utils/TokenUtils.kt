package minerva.android.walletmanager.utils

import java.util.*

object TokenUtils {
    fun generateTokenHash(chainId: Int, address: String): String = "$chainId$address".toLowerCase(Locale.ROOT)
}