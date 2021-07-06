package minerva.android.walletConnect.client

import minerva.android.kotlinUtils.function.orElse
import okhttp3.Response
import retrofit2.HttpException

fun Response?.isForceTerminationError(): Boolean = this?.code?.isServerError().orElse { false }
fun Exception.isForceTerminationError(): Boolean = this is HttpException && this.code().isServerError()
fun Throwable.isForceTerminationError(): Boolean = this is HttpException && this.code().isServerError()
fun Int.isServerError(): Boolean =
    (this.toString().first() == HTTP_4XX_PREFIX || this.toString().first() == HTTP_5XX_PREFIX)

private const val HTTP_4XX_PREFIX = '4'
private const val HTTP_5XX_PREFIX = '5'