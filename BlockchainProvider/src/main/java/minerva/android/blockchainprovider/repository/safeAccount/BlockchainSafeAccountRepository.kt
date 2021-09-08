package minerva.android.blockchainprovider.repository.safeAccount

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.TransactionPayload

interface BlockchainSafeAccountRepository {
    fun deployGnosisSafeContract(privateKey: String, address: String, chainId: Int): Single<String>
    fun getGnosisSafeOwners(contractAddress: String, chainId: Int, privateKey: String): Single<List<String>>
    fun addSafeAccountOwner(owner: String, gnosisAddress: String, chainId: Int, privateKey: String): Completable
    fun removeSafeAccountOwner(removeAddress: String, gnosisAddress: String, chainId: Int, privateKey: String): Completable
    fun transferNativeCoin(chainId: Int, transactionPayload: TransactionPayload): Completable
    fun transferERC20Token(chainId: Int, transactionPayload: TransactionPayload, tokenAddress: String): Completable
}