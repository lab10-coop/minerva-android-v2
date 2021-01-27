package minerva.android.walletmanager.repository.smartContract

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function3
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.smartContract.BlockchainSafeAccountRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.model.Recipient
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.mappers.TransactionMapper
import minerva.android.walletmanager.storage.LocalStorage
import java.math.BigInteger

class SmartContractRepositoryImpl(
    private val blockchainSafeAccountRepository: BlockchainSafeAccountRepository,
    private val blockchainRegularAccountRepository: BlockchainRegularAccountRepository,
    private val localStorage: LocalStorage,
    private val walletConfigManager: WalletConfigManager
) : SmartContractRepository {

    override fun createSafeAccount(account: Account) =
        blockchainSafeAccountRepository.deployGnosisSafeContract(
            account.privateKey,
            account.address,
            account.network.short
        )

    override fun getSafeAccountOwners(
        contractAddress: String,
        network: String,
        privateKey: String,
        account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.getGnosisSafeOwners(contractAddress, network, privateKey)
            .flatMap { walletConfigManager.updateSafeAccountOwners(account.id, it) }

    override fun addSafeAccountOwner(
        owner: String,
        address: String,
        network: String,
        privateKey: String,
        account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.addSafeAccountOwner(owner, address, network, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(account.id, prepareAddedOwnerList(owner, account)))

    override fun removeSafeAccountOwner(
        removeAddress: String, address: String,
        network: String, privateKey: String, account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.removeSafeAccountOwner(removeAddress, address, network, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(account.id, prepareRemovedOwnerList(removeAddress, account)))

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable =
        blockchainSafeAccountRepository.transferNativeCoin(network, TransactionMapper.map(transaction))
            .andThen(blockchainRegularAccountRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable =
        blockchainSafeAccountRepository.transferERC20Token(network, TransactionMapper.map(transaction), erc20Address)
            .andThen(blockchainRegularAccountRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> =
        walletConfigManager.removeSafeAccountOwner(index, owner)

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String =
        walletConfigManager.getSafeAccountMasterOwnerPrivateKey(address)

    override fun getERC20TokenDetails(privateKey: String, network: String, tokenAddress: String): Single<Token> =
        (blockchainRegularAccountRepository).run {
            Observable.zip(
                getERC20TokenName(privateKey, network, tokenAddress),
                getERC20TokenSymbol(privateKey, network, tokenAddress),
                getERC20TokenDecimals(privateKey, network, tokenAddress),
                Function3<String, String, BigInteger, Token> { name, symbol, decimals ->
                    Token(name, symbol, tokenAddress, decimals.toString())
                }
            ).firstOrError()
        }

    private fun prepareAddedOwnerList(owner: String, account: Account): List<String> {
        account.owners?.toMutableList()?.let {
            it.add(FIRST_POSITION, owner)
            return it
        }
        return emptyList()
    }

    private fun prepareRemovedOwnerList(removeAddress: String, account: Account): List<String> {
        account.owners?.toMutableList()?.let {
            it.remove(removeAddress)
            return it
        }
        return emptyList()
    }

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    companion object {
        private const val FIRST_POSITION = 0
    }
}