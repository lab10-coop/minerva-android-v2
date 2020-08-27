package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.exception.MissingAccountThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
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
    private val blockchainRepository: BlockchainRepository
) : AccountManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun createAccount(network: Network, accountName: String, ownerAddress: String, contract: String): Completable {
        with(walletConfigManager) {
            getWalletConfig()?.let { config ->
                val newAccount = Account(config.newIndex, name = accountName, network = network.short, bindedOwner = ownerAddress)
                return cryptographyRepository.computeDeliveredKeys(masterSeed.seed, newAccount.index)
                    .map { createUpdatedWalletConfig(config, newAccount, it, ownerAddress, contract) }
                    .flatMapCompletable { updateWalletConfig(it) }
            }
            return Completable.error(NotInitializedWalletConfigThrowable())
        }
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
            return WalletConfig(updateVersion, identities, newAccounts, services, credentials)
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
            val newValues = it.accounts.toMutableList()
            it.accounts.forEachIndexed { position, value ->
                if (value.index == index) {
                    when {
                        areFundsOnValue(value.cryptoBalance, value.accountAssets) || hasMoreOwners(value) ->
                            return Completable.error(BalanceIsNotEmptyAndHasMoreOwnersThrowable())
                        isNotSafeAccountMasterOwner(it.accounts, value) ->
                            return Completable.error(IsNotSafeAccountMasterOwnerThrowable())
                    }
                    newValues[position] = Account(value, true)
                    return walletConfigManager.updateWalletConfig(
                        WalletConfig(it.updateVersion, it.identities, newValues, it.services, it.credentials)
                    )
                }
            }
            return Completable.error(MissingAccountThrowable())
        }
        return Completable.error(NotInitializedWalletConfigThrowable())
    }

    private fun isNotSafeAccountMasterOwner(accounts: List<Account>, account: Account): Boolean {
        account.owners?.let {
            accounts.forEach { if (it.address == account.masterOwnerAddress) return false }
            return true
        }
        return false
    }

    private fun hasMoreOwners(account: Account): Boolean {
        account.owners?.let { return it.size > 1 }
        return false
    }

    private fun areFundsOnValue(balance: BigDecimal, accountAssets: List<AccountAsset>): Boolean {
        accountAssets.forEach {
            if (blockchainRepository.toGwei(it.balance) >= MAX_GWEI_TO_REMOVE_VALUE) return true
        }
        return blockchainRepository.toGwei(balance) >= MAX_GWEI_TO_REMOVE_VALUE
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