package minerva.android.values.listener

import minerva.android.kotlinUtils.Empty

interface BaseScannerListener {
    fun onBackPressed()
    fun onResult(isResultSucceed: Boolean, message: String? = String.Empty)
}