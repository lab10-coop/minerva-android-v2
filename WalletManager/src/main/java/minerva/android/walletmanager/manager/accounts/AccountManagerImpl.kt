package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
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
import minerva.android.cryptographyProvider.repository.model.DerivationPath
import minerva.android.walletmanager.utils.CryptoUtils
import java.math.BigDecimal
import java.math.BigInteger

class AccountManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRegularAccountRepository
) : AccountManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletManager.walletConfigLiveData

    override fun createRegularAccount(network: Network): Single<String> {
        walletManager.getWalletConfig()?.let { config ->
            val (index, derivationPath) = getIndexWithDerivationPath(network, config)
            val accountName = CryptoUtils.prepareName(network, index)
            return cryptographyRepository.calculateDerivedKeys(
                walletManager.masterSeed.seed,
                index,
                derivationPath,
                network.testNet
            )
                .map { keys ->
                    val newAccount = Account(
                        index,
                        name = accountName,
                        network = network,
                        publicKey = keys.publicKey,
                        privateKey = keys.privateKey,
                        address = keys.address
                    )
                    addAccount(newAccount, config)
                }
                .flatMapCompletable { walletManager.updateWalletConfig(it) }.toSingleDefault(accountName)
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun addAccount(newAccount: Account, config: WalletConfig): WalletConfig {
        val newAccounts = config.accounts.toMutableList()
        newAccounts.add(config.accounts.size, newAccount)
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    override fun createSafeAccount(account: Account, contract: String): Completable {
        walletManager.getWalletConfig()?.let { config ->
            val (index, derivationPath) = getIndexWithDerivationPath(account.network, config)
            return cryptographyRepository.calculateDerivedKeys(
                walletManager.masterSeed.seed,
                index,
                derivationPath,
                account.network.testNet
            )
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
                .flatMapCompletable { walletManager.updateWalletConfig(it) }
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


    override val areMainNetworksEnabled: Boolean
        get() = walletManager.areMainNetworksEnabled

    override var toggleMainNetsEnabled: Boolean?
        get() = walletManager.toggleMainNetsEnabled
        set(value) {
            walletManager.toggleMainNetsEnabled = value
        }

    override val enableMainNetsFlowable: Flowable<Boolean>
        get() = walletManager.enableMainNetsFlowable

    override fun removeAccount(account: Account): Completable {
        walletManager.getWalletConfig()?.let { config ->
            val newAccounts: MutableList<Account> = config.accounts.toMutableList()
            val accountIndex = newAccounts.indexOf(account)
            config.accounts.forEachIndexed { index, item ->
                if (index == accountIndex) {
                    return when {
                        areFundsOnValue(item.cryptoBalance, item.accountAssets) -> handleNoFundsError(item)
                        isNotSafeAccountMasterOwner(config.accounts, item) ->
                            Completable.error(IsNotSafeAccountMasterOwnerThrowable())
                        else -> {
                            newAccounts[index] = Account(item, true)
                            walletManager.updateWalletConfig(config.copy(version = config.updateVersion, accounts = newAccounts))
                        }
                    }
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
        else walletManager.getSafeAccountNumber(ownerAddress)

    override fun loadAccount(index: Int): Account {
        walletManager.getWalletConfig()?.accounts?.apply {
            return if (inBounds(index)) {
                val account = this[index]
                account.copy(address = blockchainRepository.toChecksumAddress(account.address))
            } else Account(Int.InvalidIndex)
        }
        return Account(Int.InvalidIndex)
    }

    companion object {
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private const val NO_SAFE_ACCOUNTS = 0
    }
}