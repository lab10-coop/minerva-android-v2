package minerva.android.walletmanager.manager

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Recipient
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.mappers.TransactionMapper
import minerva.android.walletmanager.storage.LocalStorage

class SmartContractManagerImpl(
    private val smartContractRepository: SmartContractRepository,
    private val blockchainRepository: BlockchainRepository,
    private val localStorage: LocalStorage
) : SmartContractManager {

    override fun createSafeAccount(value: Value) =
        smartContractRepository.deployGnosisSafeContract(value.privateKey, value.address, value.network)

    override fun getSafeAccountOwners(contractAddress: String, network: String, privateKey: String): Single<List<String>> =
        smartContractRepository.getGnosisSafeOwners(contractAddress, network, privateKey)

    override fun addSafeAccountOwner(owner: String, gnosisAddress: String, network: String, privateKey: String): Completable =
        smartContractRepository.addSafeAccountOwner(owner, gnosisAddress, network, privateKey)

    override fun removeSafeAccountOwner(removeAddress: String, gnosisAddress: String, network: String, privateKey: String): Completable =
        smartContractRepository.removeSafeAccountOwner(removeAddress, gnosisAddress, network, privateKey)

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable =
        smartContractRepository.transferNativeCoin(network, TransactionMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()


    override fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable =
        smartContractRepository.transferERC20Token(network, TransactionMapper.map(transaction), erc20Address)
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))
}