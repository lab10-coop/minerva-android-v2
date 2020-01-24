package minerva.android.values.listener

interface BaseScannerListener {
    fun onBackPressed()
    fun onResult(isResultSucceed: Boolean)
}