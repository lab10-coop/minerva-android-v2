package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.exception.*
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.DerivationPath
import minerva.android.walletmanager.utils.CryptoUtils
import java.math.BigDecimal
import java.math.BigInteger

class AccountManagerImpl(
        private val walletConfigManager: WalletConfigManager,
        private val cryptographyRepository: CryptographyRepository,
        private val blockchainRepository: BlockchainRegularAccountRepository
) : AccountManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun createRegularAccount(network: Network): Completable {
        walletConfigManager.getWalletConfig()?.let { config ->
            val (index, derivationPath) = getIndexWithDerivationPath(network, config)
            return cryptographyRepository.calculateDerivedKeys(walletConfigManager.masterSeed.seed, index, derivationPath)
                    .map { keys ->
                        val newAccount = Account(
                                index,
                                name = CryptoUtils.prepareName(network, index),
                                network = network,
                                publicKey = keys.publicKey,
                                privateKey = keys.privateKey,
                                address = keys.address
                        )
                        addAccount(newAccount, config)
                    }
                    .flatMapCompletable { walletConfigManager.updateWalletConfig(it) }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun addAccount(newAccount: Account, config: WalletConfig): WalletConfig {
        val newAccounts = config.accounts.toMutableList()
        newAccounts.add(config.accounts.size, newAccount)
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    override fun createSafeAccount(account: Account, contract: String): Completable {
        walletConfigManager.getWalletConfig()?.let { config ->
            val (index, derivationPath) = getIndexWithDerivationPath(account.network, config)
            return cryptographyRepository.calculateDerivedKeys(walletConfigManager.masterSeed.seed, index, derivationPath)
                    .map { keys ->
                        val ownerAddress = account.address
                        val newAccount = Account(
                                index,
                                name = getSafeAccountName(account),
                                network = account.network,
                                bindedOwner = ownerAddress,
                                publicKey = keys.publicKey,
                                privateKey = keys.privateKey,
                                address = contract,
                                contractAddress = contract,
                                owners = mutableListOf(ownerAddress)
                        )
                        addSafeAccount(config, newAccount, ownerAddress)
                    }
                    .flatMapCompletable { walletConfigManager.updateWalletConfig(it) }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun getIndexWithDerivationPath(network: Network, config: WalletConfig): Pair<Int, String> =
            if (network.testNet) {
                Pair(config.newTestNetworkIndex, DerivationPath.TEST_NET_PATH)
            } else {
                Pair(config.newMainNetworkIndex, DerivationPath.MAIN_NET_PATH)
            }

    private fun addSafeAccount(config: WalletConfig, newAccount: Account, ownerAddress: String): WalletConfig {
        val newAccounts = config.accounts.toMutableList()
        var newAccountPosition = config.accounts.size
        config.accounts.forEachIndexed { position, account ->
            if (account.address == ownerAddress)
                newAccountPosition = position + getSafeAccountCount(ownerAddress)
        }
        newAccounts.add(newAccountPosition, newAccount)
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    override fun getSafeAccountName(account: Account): String =
            account.name.replaceFirst(String.Space, " | ${getSafeAccountCount(account.address)} ")

    override fun isAddressValid(address: String): Boolean =
            blockchainRepository.isAddressValid(address)

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