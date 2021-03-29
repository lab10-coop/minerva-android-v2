package minerva.android.walletmanager.manager

import androidx.lifecycle.LiveData
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.wallet.WalletConfig

interface Manager {
    val walletConfigLiveData: LiveData<Event<WalletConfig>>
}