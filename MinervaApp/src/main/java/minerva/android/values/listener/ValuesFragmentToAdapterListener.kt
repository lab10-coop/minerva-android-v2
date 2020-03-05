package minerva.android.values.listener

import minerva.android.walletmanager.model.Value

interface ValuesFragmentToAdapterListener {
    fun onSendTransaction(value: Value)
    fun onSendAssetTransaction(valueIndex: Int, assetIndex: Int)
    fun onCreateSafeAccount(value: Value)
    fun onValueRemove(value: Value)
}