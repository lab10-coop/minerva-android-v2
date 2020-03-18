package minerva.android.walletmanager.manager

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.mappers.TransactionMapper

class SmartContractManagerImpl(private val smartContractRepository: SmartContractRepository) : SmartContractManager {

    override fun createSafeAccount(value: Value) =
        smartContractRepository.deployGnosisSafeContract(value.privateKey, value.address, value.network)

    override fun getSafeAccountOwners(contractAddress: String, network: String, privateKey: String): Single<List<String>> =
        smartContractRepository.getGnosisSafeOwners(contractAddress, network, privateKey)

    override fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable =
        smartContractRepository.addSafeAccountOwner(owner, gnosisAddress, network, privateKey)

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable =
        smartContractRepository.transferNativeCoin(network, TransactionMapper.map(transaction))

    override fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable =
        smartContractRepository.transferERC20Token(network, TransactionMapper.map(transaction), erc20Address)
}