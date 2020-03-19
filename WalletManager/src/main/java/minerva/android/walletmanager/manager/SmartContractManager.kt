package minerva.android.walletmanager.manager

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value

interface SmartContractManager {
    fun createSafeAccount(value: Value): Single<String>
    fun transferNativeCoin(network: String, transaction: Transaction): Completable
    fun getSafeAccountOwners(gnosisAddress: String, network: String, privateKey: String): Single<List<String>>
    fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable
    fun removeSafeAccountOwner(removeAddress: String, gnosisAddress: String, network: String, privateKey: String): Completable
    fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable
}