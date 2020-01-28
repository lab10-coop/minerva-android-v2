package minerva.android.main.listener

import minerva.android.walletmanager.model.Value

interface FragmentInteractorListener {
    fun showSendTransactionScreen(value: Value)
}