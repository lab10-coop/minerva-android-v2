package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivationPath
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.exception.MissingAccountThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.storage.LocalStorage
import java.math.BigDecimal
import java.math.BigInteger

class AccountManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val localStorage: LocalStorage,
    private val timeProvider: CurrentTimeProvider //TODO make one class with DateUtils
) : AccountManager {
    override var hasAvailableAccounts: Boolean = false
    override var activeAccounts: List<Account> = emptyList()
    override var cachedTokens: Map<Int, List<ERC20Token>> = mapOf()
    override val areMainNetworksEnabled: Boolean get() = walletManager.areMainNetworksEnabled
    override val isProtectKeysEnabled: Boolean get() = localStorage.isProtectKeysEnabled
    override val isProtectTransactionsEnabled: Boolean get() = localStorage.isProtectTransactionsEnabled
    override val masterSeed: MasterSeed get() = walletManager.masterSeed
    override val getTokenVisibilitySettings: TokenVisibilitySettings get() = localStorage.getTokenVisibilitySettings()
    override var showMainNetworksWarning: Boolean
        get() = walletManager.showMainNetworksWarning
        set(value) {
            walletManager.showMainNetworksWarning = value
        }
    override val walletConfigLiveData: LiveData<Event<WalletConfig>>
        get() = Transformations.map(walletManager.walletConfigLiveData) { walletConfigEvent ->
            with(walletConfigEvent.peekContent()) {
                hasAvailableAccounts = hasActiveAccount
                activeAccounts = getActiveAccounts(this)
                cachedTokens = filterCachedTokens(erc20Tokens)
            }
            walletConfigEvent
        }

    internal fun getActiveAccounts(walletConfig: WalletConfig): List<Account> =
        walletConfig.accounts.filter { account -> !account.isDeleted && account.network.testNet == !areMainNetworksEnabled }

    override fun createRegularAccount(network: Network): Single<String> =
        walletManager.getWalletConfig().run {
            val (index, derivationPath) = getIndexWithDerivationPath(network, this)
            val accountName = CryptoUtils.prepareName(network.name, index)
            cryptographyRepository.calculateDerivedKeys(
                walletManager.masterSeed.seed,
                index, derivationPath, network.testNet
            ).map { keys ->
                val newAccount = Account(
                    index,
                    name = accountName,
                    chainId = network.chainId,
                    publicKey = keys.publicKey,
                    privateKey = keys.privateKey,
                    address = keys.address
                )
                addAccount(newAccount, this)
            }.flatMapCompletable { config -> walletManager.updateWalletConfig(config) }.toSingleDefault(accountName)
        }

    private fun addAccount(newAccount: Account, config: WalletConfig): WalletConfig {
        val newAccounts = config.accounts.toMutableList()
        newAccounts.add(config.accounts.size, newAccount)
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    override fun createSafeAccount(account: Account, contract: String): Completable =
        walletManager.getWalletConfig().run {
            val (index, derivationPath) = getIndexWithDerivationPath(account.network, this)
            return cryptographyRepository.calculateDerivedKeys(
                walletManager.masterSeed.seed,
                index, derivationPath, account.network.testNet
            ).map { keys ->
                val ownerAddress = account.address
                val newAccount = Account(
                    index,
                    name = getSafeAccountName(account),
                    chainId = account.network.chainId,
                    bindedOwner = ownerAddress,
                    publicKey = keys.publicKey,
                    privateKey = keys.privateKey,
                    address = contract,
                    contractAddress = contract,
                    owners = mutableListOf(ownerAddress)
                )
                addSafeAccount(this, newAccount, ownerAddress)
            }.flatMapCompletable { walletManager.updateWalletConfig(it) }
        }

    internal fun filterCachedTokens(tokenMap: Map<Int, List<ERC20Token>>): Map<Int, List<ERC20Token>> {
        val newMap: MutableMap<Int, MutableList<ERC20Token>> = mutableMapOf()
        activeAccounts.filter { account -> !account.isPending }.forEach { account ->
            if (newMap.containsKey(account.chainId)) {
                newMap[account.chainId]?.addAll(getVisibleTokensList(tokenMap, account))
            } else {
                newMap[account.chainId] = getVisibleTokensList(tokenMap, account).toMutableList()
            }
        }
        return newMap
    }

    private fun getVisibleTokensList(tokenMap: Map<Int, List<ERC20Token>>, account: Account): List<ERC20Token> =
        tokenMap[account.chainId]?.let { tokens ->
            tokens.filter { token ->
                token.accountAddress.equals(account.address, true) &&
                        getTokenVisibilitySettings.getTokenVisibility(account.address, token.address) == true
            }
        } ?: emptyList()

    private fun getIndexWithDerivationPath(
        network: Network,
        config: WalletConfig
    ): Pair<Int, String> = if (network.testNet) {
        Pair(config.newTestNetworkIndex, DerivationPath.TEST_NET_PATH)
    } else {
        Pair(config.newMainNetworkIndex, DerivationPath.MAIN_NET_PATH)
    }

    private fun addSafeAccount(
        config: WalletConfig,
        newAccount: Account,
        ownerAddress: String
    ): WalletConfig {
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

    override fun isAddressValid(address: String): Boolean = blockchainRepository.isAddressValid(address)

    override fun saveFreeATSTimestamp() {
        localStorage.saveFreeATSTimestamp(timeProvider.currentTimeMills())
    }

    override fun getLastFreeATSTimestamp(): Long = localStorage.loadLastFreeATSTimestamp()

    override fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings =
        localStorage.saveTokenVisibilitySettings(settings)

    override fun currentTimeMills(): Long = timeProvider.currentTimeMills()

    override fun getAllAccounts(): List<Account> = walletManager.getWalletConfig().accounts

    override fun getAllActiveAccounts(chainId: Int): List<Account> =
        getAllAccounts().filter { account -> !account.isDeleted && account.chainId == chainId }

    override fun toChecksumAddress(address: String): String = blockchainRepository.toChecksumAddress(address)

    override fun clearFiat() =
        walletManager.getWalletConfig().accounts.forEach { account ->
            account.fiatBalance = Double.InvalidValue.toBigDecimal()
            account.accountTokens.forEach { accountToken -> accountToken.tokenPrice = Double.InvalidValue }
        }

    override fun removeAccount(account: Account): Completable =
        walletManager.getWalletConfig().run {
            val newAccounts: MutableList<Account> = accounts.toMutableList()
            accounts.forEachIndexed { index, item ->
                if (item.address == account.address) {
                    return handleRemovingAccount(item, this, newAccounts, index)
                }
            }
            Completable.error(MissingAccountThrowable())
        }

    private fun handleRemovingAccount(
        item: Account, config: WalletConfig,
        newAccounts: MutableList<Account>, index: Int
    ): Completable =
        when {
            areFundsOnValue(item.cryptoBalance, item.accountTokens) -> handleNoFundsError(item)
            isNotSAMasterOwner(config.accounts, item) -> Completable.error(IsNotSafeAccountMasterOwnerThrowable())
            else -> {
                newAccounts[index] = Account(item, true)
                walletManager.updateWalletConfig(config.copy(version = config.updateVersion, accounts = newAccounts))
            }
        }

    private fun handleNoFundsError(account: Account): Completable =
        if (account.isSafeAccount) {
            Completable.error(BalanceIsNotEmptyAndHasMoreOwnersThrowable())
        } else {
            Completable.error(BalanceIsNotEmptyThrowable())
        }

    private fun isNotSAMasterOwner(accounts: List<Account>, account: Account): Boolean {
        account.owners?.let {
            accounts.forEach { if (it.address == account.masterOwnerAddress) return false }
            return true
        }
        return false
    }

    private fun areFundsOnValue(balance: BigDecimal, accountTokens: List<AccountToken>): Boolean {
        accountTokens.forEach { accountToken ->
            if (accountToken.rawBalance.toBigInteger() >= MAX_GWEI_TO_REMOVE_VALUE) return true
        }
        return blockchainRepository.toGwei(balance).toBigInteger() >= MAX_GWEI_TO_REMOVE_VALUE
    }

    override fun getSafeAccountCount(ownerAddress: String): Int =
        if (ownerAddress == String.Empty) NO_SAFE_ACCOUNTS
        else walletManager.getSafeAccountNumber(ownerAddress)

    override fun loadAccount(index: Int): Account = walletManager.getWalletConfig().accounts.run {
        if (inBounds(index)) {
            val account = this[index]
            account.copy(address = blockchainRepository.toChecksumAddress(account.address))
        } else Account(Int.InvalidIndex)
    }

    companion object {
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private const val NO_SAFE_ACCOUNTS = 0
        const val NEW_ACCOUNT_TITLE_PATTERN = "%s #%d"
    }
}