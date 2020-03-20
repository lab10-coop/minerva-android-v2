package minerva.android.values.listener

import minerva.android.kotlinUtils.Empty
import minerva.android.services.login.uitls.LoginPayload

interface BaseScannerListener {
    fun onBackPressed()
    fun onResult(
        isResultSucceed: Boolean,
        message: String? = String.Empty,
        loginPayload: LoginPayload? = null
    )
}