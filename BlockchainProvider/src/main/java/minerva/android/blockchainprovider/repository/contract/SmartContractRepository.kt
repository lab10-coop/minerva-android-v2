package minerva.android.blockchainprovider.repository.contract

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.TransactionPayload

interface SmartContractRepository {
    fun deployGnosisSafeContract(privateKey: String, address: String, network: String): Single<String>
    fun transferNativeCoin(network: String, transactionPayload: TransactionPayload): Completable
}