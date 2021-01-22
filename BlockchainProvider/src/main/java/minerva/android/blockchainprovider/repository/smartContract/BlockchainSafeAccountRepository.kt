package minerva.android.blockchainprovider.repository.smartContract

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.TransactionPayload

interface BlockchainSafeAccountRepository {
    fun deployGnosisSafeContract(privateKey: String, address: String, network: String): Single<String>
    fun getGnosisSafeOwners(contractAddress: String, network: String, privateKey: String): Single<List<String>>
    fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable
    fun removeSafeAccountOwner(removeAddress: String, gnosisAddress: String, network: String, privateKey: String): Completable
    fun transferNativeCoin(network: String, transactionPayload: TransactionPayload): Completable
    fun transferERC20Token(network: String, transactionPayload: TransactionPayload, tokenAddress: String): Completable
}