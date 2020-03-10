package minerva.android.blockchainprovider.repository.contract

import io.reactivex.Single

interface SmartContractRepository {
    fun deployGnosisSafeContract(privateKey: String, address: String, network: String): Single<String>
}