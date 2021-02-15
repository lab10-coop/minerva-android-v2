package minerva.android.configProvider.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor(val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(AUTH_TOKEN, token)
        return chain.proceed(request.build())
    }

    companion object {
        private const val AUTH_TOKEN = "X-AUTH-TOKEN"
    }
}