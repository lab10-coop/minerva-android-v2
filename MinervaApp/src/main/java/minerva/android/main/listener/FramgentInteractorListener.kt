package minerva.android.main.listener

import minerva.android.walletmanager.model.Value

interface FramgentInteractorListener {
    fun showSendTransactonScreen(value: Value)
}