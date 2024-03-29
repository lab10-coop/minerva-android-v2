package minerva.android.walletmanager.repository.smartContract

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function3
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.safeAccount.BlockchainSafeAccountRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.mappers.TransactionMapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.storage.LocalStorage
import java.math.BigDecimal
import java.math.BigInteger

class SafeAccountRepositoryImpl(
    private val blockchainSafeAccountRepository: BlockchainSafeAccountRepository,
    private val erC20TokenRepository: ERC20TokenRepository,
    private val ensRepository: ENSRepository,
    private val localStorage: LocalStorage,
    private val walletConfigManager: WalletConfigManager
) : SafeAccountRepository {

    override fun createSafeAccount(account: Account) =
        blockchainSafeAccountRepository.deployGnosisSafeContract(
            account.privateKey,
            account.address,
            account.network.chainId
        )

    override fun getSafeAccountOwners(
        contractAddress: String,
        chainId: Int,
        privateKey: String,
        account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.getGnosisSafeOwners(contractAddress, chainId, privateKey)
            .flatMap { walletConfigManager.updateSafeAccountOwners(account.id, it) }

    override fun addSafeAccountOwner(
        owner: String,
        address: String,
        chainId: Int,
        privateKey: String,
        account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.addSafeAccountOwner(owner, address, chainId, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(account.id, prepareAddedOwnerList(owner, account)))

    override fun removeSafeAccountOwner(
        removeAddress: String, address: String,
        chainId: Int, privateKey: String, account: Account
    ): Single<List<String>> =
        blockchainSafeAccountRepository.removeSafeAccountOwner(removeAddress, address, chainId, privateKey)
            .andThen(walletConfigManager.updateSafeAccountOwners(account.id, prepareRemovedOwnerList(removeAddress, account)))

    override fun transferNativeCoin(chainId: Int, transaction: Transaction): Completable =
        blockchainSafeAccountRepository.transferNativeCoin(chainId, TransactionMapper.map(transaction))
            .andThen(ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun transferERC20Token(chainId: Int, transaction: Transaction, erc20Address: String): Completable =
        blockchainSafeAccountRepository.transferERC20Token(chainId, TransactionMapper.map(transaction), erc20Address)
            .andThen(ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> =
        walletConfigManager.removeSafeAccountOwner(index, owner)

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String =
        walletConfigManager.getSafeAccountMasterOwnerPrivateKey(address)

    override fun getSafeAccountMasterOwnerBalance(address: String?): BigDecimal =
        walletConfigManager.getSafeAccountMasterOwnerBalance(address)

    override fun getERC20TokenDetails(privateKey: String, chainId: Int, tokenAddress: String): Single<ERCToken> =
        (erC20TokenRepository).run {
            Observable.zip(
                getERC20TokenName(privateKey, chainId, tokenAddress),
                getERC20TokenSymbol(privateKey, chainId, tokenAddress),
                getERC20TokenDecimals(privateKey, chainId, tokenAddress),
                Function3<String, String, BigInteger, ERCToken> { name, symbol, decimals ->
                    ERCToken(
                        chainId = chainId,
                        name = name,
                        symbol = symbol,
                        address = tokenAddress,
                        decimals = decimals.toString(),
                        type = TokenType.ERC20
                    )
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