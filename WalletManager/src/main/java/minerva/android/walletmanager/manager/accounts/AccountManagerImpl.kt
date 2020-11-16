package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.exception.*
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
import java.math.BigDecimal
import java.math.BigInteger

class AccountManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRegularAccountRepository
) : AccountManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun createAccount(network: Network, accountName: String, ownerAddress: String, contract: String): Completable {
        walletConfigManager.getWalletConfig()?.let { config ->
            val newAccount = Account(config.newIndex, name = accountName, network = network, bindedOwner = ownerAddress)
            return cryptographyRepository.computeDeliveredKeys(walletConfigManager.masterSeed.seed, newAccount.index)
                .map { createUpdatedWalletConfig(config, newAccount, it, ownerAddress, contract) }
                .flatMapCompletable { walletConfigManager.updateWalletConfig(it) }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun createUpdatedWalletConfig(
        config: WalletConfig, newAccount: Account,
        derivedKeys: DerivedKeys, ownerAddress: String,
        contractAddress: String
    ): WalletConfig {
        prepareNewValue(newAccount, derivedKeys, ownerAddress, contractAddress)
        config.run {
            val newAccounts = accounts.toMutableList()
            var newValuePosition = accounts.size
            accounts.forEachIndexed { position, value ->
                if (value.address == ownerAddress && ownerAddress != String.Empty)
                    newValuePosition = position + getSafeAccountCount(ownerAddress)
            }
            newAccounts.add(newValuePosition, newAccount)
            return this.copy(version = updateVersion, accounts = newAccounts)
        }
    }

    private fun prepareNewValue(newAccount: Account, derivedKeys: DerivedKeys, ownerAddress: String, contractAddress: String) {
        newAccount.apply {
            publicKey = derivedKeys.publicKey
            privateKey = derivedKeys.privateKey
            if (ownerAddress.isNotEmpty()) owners = mutableListOf(ownerAddress)
            address = if (contractAddress.isNotEmpty()) {
                this.contractAddress = contractAddress
                contractAddress
            } else derivedKeys.address
        }
    }

    override fun removeAccount(index: Int): Completable {
        walletConfigManager.getWalletConfig()?.let {
            val newAccounts = it.accounts.toMutableList()
            it.accounts.forEachIndexed { position, account ->
                if (account.index == index) {
                    when {
                        areFundsOnValue(account.cryptoBalance, account.accountAssets) ->
                            return handleNoFundsError(account)
                        isNotSafeAccountMasterOwner(it.accounts, account) ->
                            return Completable.error(IsNotSafeAccountMasterOwnerThrowable())
                    }
                    newAccounts[position] = Account(account, true)
                    return walletConfigManager.updateWalletConfig(it.copy(version = it.updateVersion, accounts = newAccounts))
                }
            }
            return Completable.error(MissingAccountThrowable())
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun handleNoFundsError(account: Account): Completable =
        if (account.isSafeAccount) {
            Completable.error(BalanceIsNotEmptyAndHasMoreOwnersThrowable())
        } else {
            Completable.error(BalanceIsNotEmptyThrowable())
        }

    private fun isNotSafeAccountMasterOwner(accounts: List<Account>, account: Account): Boolean {
        account.owners?.let {
            accounts.forEach { if (it.address == account.masterOwnerAddress) return false }
            return true
        }
        return false
    }

    private fun areFundsOnValue(balance: BigDecimal, accountAssets: List<AccountAsset>): Boolean {
        accountAssets.forEach {
            if (blockchainRepository.toGwei(it.balance).toBigInteger() >= MAX_GWEI_TO_REMOVE_VALUE) return true
        }
        return blockchainRepository.toGwei(balance).toBigInteger() >= MAX_GWEI_TO_REMOVE_VALUE
    }

    override fun getSafeAccountCount(ownerAddress: String): Int =
        if (ownerAddress == String.Empty) NO_SAFE_ACCOUNTS
        else walletConfigManager.getSafeAccountNumber(ownerAddress)

    override fun loadAccount(position: Int): Account {
        walletConfigManager.getWalletConfig()?.accounts?.apply {
            return if (inBounds(position)) this[position]
            else Account(Int.InvalidIndex)
        }
        return Account(Int.InvalidIndex)
    }

    companion object {
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private const val NO_SAFE_ACCOUNTS = 0
    }
}