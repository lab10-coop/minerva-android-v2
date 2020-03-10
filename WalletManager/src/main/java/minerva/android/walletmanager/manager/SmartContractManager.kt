package minerva.android.walletmanager.manager

import io.reactivex.Single
import minerva.android.walletmanager.model.Value

interface SmartContractManager {
    fun createSafeAccount(value: Value): Single<String>
}