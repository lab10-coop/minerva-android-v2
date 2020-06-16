package minerva.android.walletmanager.smartContract

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.contract.BlockchainContractRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Recipient
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.mappers.TransactionMapper
import minerva.android.walletmanager.storage.LocalStorage

class SmartContractRepositoryImpl(
    private val blockchainContractRepository: BlockchainContractRepository,
    private val blockchainRepository: BlockchainRepository,
    private val localStorage: LocalStorage,
    private val walletConfigManager: WalletConfigManager
) : SmartContractRepository {

    override fun createSafeAccount(value: Value) =
        blockchainContractRepository.deployGnosisSafeContract(value.privateKey, value.address, value.network)

    override fun getSafeAccountOwners(contractAddress: String, network: String, privateKey: String, value: Value): Single<List<String>> =
        blockchainContractRepository.getGnosisSafeOwners(contractAddress, network, privateKey)
            .flatMap { walletConfigManager.updateSafeAccountOwners(value.index, it) }

    override fun addSafeAccountOwner(owner: String, address: String, network: String, privateKey: String, value: Value): Single<List<String>> =
        blockchainContractRepository.addSafeAccountOwner(owner, address, network, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(value.index, prepareAddedOwnerList(owner, value)))

    private fun prepareAddedOwnerList(owner: String, value: Value): List<String> {
        value.owners?.toMutableList()?.let {
            it.add(FIRST_POSITION, owner)
            return it
        }
        throw IllegalStateException("Owners Live Data was not initialized")
    }

    override fun removeSafeAccountOwner(
        removeAddress: String, address: String,
        network: String, privateKey: String, value: Value
    ): Single<List<String>> =
        blockchainContractRepository.removeSafeAccountOwner(removeAddress, address, network, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(value.index, prepareRemovedOwnerList(removeAddress, value)))

    private fun prepareRemovedOwnerList(removeAddress: String, value: Value): List<String> {
        value.owners?.toMutableList()?.let {
            it.remove(removeAddress)
            return it
        }
        throw IllegalStateException("Owners Live Data was not initialized")
    }

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable =
        blockchainContractRepository.transferNativeCoin(network, TransactionMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()


    override fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable =
        blockchainContractRepository.transferERC20Token(network, TransactionMapper.map(transaction), erc20Address)
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String =
        walletConfigManager.getSafeAccountMasterOwnerPrivateKey(address)

    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> =
        walletConfigManager.removeSafeAccountOwner(index, owner)

    companion object {
        private const val FIRST_POSITION = 0
    }
}