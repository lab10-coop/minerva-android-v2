package minerva.android.walletConnect.providers

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpProvider {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.MINUTES)
        .readTimeout(TIMEOUT, TimeUnit.MINUTES)
        .writeTimeout(TIMEOUT, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()
}

private const val TIMEOUT: Long = 15L