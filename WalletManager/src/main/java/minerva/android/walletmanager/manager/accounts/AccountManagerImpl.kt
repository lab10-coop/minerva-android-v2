package minerva.android.walletmanager.manager.accounts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.units.UnitConverter
import minerva.android.blockchainprovider.repository.validation.ValidationRepository
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivationPath
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.database.dao.CoinBalanceDao
import minerva.android.walletmanager.database.dao.TokenBalanceDao
import minerva.android.walletmanager.database.entity.CoinBalanceEntity
import minerva.android.walletmanager.database.entity.TokenBalanceEntity
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.exception.MissingAccountThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.AddressStatus
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.TokenUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class AccountManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val localStorage: LocalStorage,
    private val unitConverter: UnitConverter,
    private val timeProvider: CurrentTimeProvider, //TODO make one class with DateUtils
    database: MinervaDatabase,
    private val validationRepository: ValidationRepository
) : AccountManager {
    override var hasAvailableAccounts: Boolean = false
    override var activeAccounts: List<Account> = emptyList()
    override var rawAccounts: List<Account> = emptyList()
    override var cachedTokens: Map<Int, List<ERCToken>> = mapOf()
    override val areMainNetworksEnabled: Boolean get() = walletManager.areMainNetworksEnabled
    override val isChangeNetworkEnabled: Boolean get() = localStorage.isChangeNetworkEnabled
    override val isProtectKeysEnabled: Boolean get() = localStorage.isProtectKeysEnabled
    override val isProtectTransactionsEnabled: Boolean get() = localStorage.isProtectTransactionsEnabled
    override val masterSeed: MasterSeed get() = walletManager.masterSeed
    override val getTokenVisibilitySettings: TokenVisibilitySettings get() = localStorage.getTokenVisibilitySettings()
    private val coinBalanceDao: CoinBalanceDao = database.coinBalanceDao()
    private val tokenBalanceDao: TokenBalanceDao = database.tokenBalanceDao()

    private val _balancesInsertLiveData = MutableLiveData<Event<Unit>>()
    override val balancesInsertLiveData: LiveData<Event<Unit>> get() = _balancesInsertLiveData

    override val walletConfigLiveData: LiveData<Event<WalletConfig>>
        get() = Transformations.map(walletManager.walletConfigLiveData) { walletConfigEvent ->
            with(walletConfigEvent.peekContent()) {
                hasAvailableAccounts = hasActiveAccount
                activeAccounts = getActiveAccounts(this)
                cachedTokens = filterCachedTokens(erc20Tokens)
                rawAccounts = accounts
                updateNftDetails(this)
            }
            walletConfigEvent
        }

    @VisibleForTesting
    fun updateNftDetails(walletConfig: WalletConfig) {
        with(walletConfig) {
            activeAccounts.forEach { account ->
                account.accountTokens.forEach { accountToken ->
                    erc20Tokens[account.chainId]
                        ?.find { ercToken ->
                            ercToken.address.equals(accountToken.token.address, true)
                                    && ercToken.tokenId == accountToken.token.tokenId
                        }?.let { ercToken ->
                            accountToken.mergeNftDetailsAfterWalletConfigUpdate(ercToken)
                        }
                }
            }
        }
    }

    internal fun getActiveAccounts(walletConfig: WalletConfig): List<Account> =
        walletConfig.accounts.filter { account -> account.shouldShow && account.isTestNetwork == !areMainNetworksEnabled }

    override fun areAllEmptyMainNetworkAccounts(): Boolean =
        walletManager.getWalletConfig().accounts.find { account -> !account.isEmptyAccount && !account.isTestNetwork } == null

    private fun getNextAvailableIndexForNetwork(network: Network): Int {
        val usedIds = getAllActiveAccounts(network.chainId).map { account -> account.id }
        return getAllAccountsForSelectedNetworksType().filter { account ->
            !account.isDeleted && !usedIds.contains(
                account.id
            )
        }
            .minByOrNull { account -> account.id }!!.id
    }

    override fun createEmptyAccounts(numberOfAccounts: Int) =
        walletManager.getWalletConfig().run {
            val newAccounts = mutableListOf<Account>()
            val (firstFreeIndex, derivationPath) = getIndexWithDerivationPath(
                !areMainNetworksEnabled,
                this
            )

            for (shift in 0 until numberOfAccounts) {
                val index = firstFreeIndex + shift
                val keys = cryptographyRepository.calculateDerivedKeys(
                    walletManager.masterSeed.seed,
                    walletManager.masterSeed.password,
                    index,
                    derivationPath,
                    !areMainNetworksEnabled
                )
                newAccounts.add(
                    Account(
                        index,
                        publicKey = keys.publicKey,
                        privateKey = keys.privateKey,
                        address = keys.address,
                        _isTestNetwork = !areMainNetworksEnabled
                    )
                )
            }
            walletManager.updateWalletConfig(getWalletConfigWithNewAccounts(newAccounts, this))
        }

    private fun createRegularAccountWithGivenIndex(index: Int, network: Network): Single<String> =
        walletManager.getWalletConfig().run {
            val derivationPath =
                if (network.testNet) DerivationPath.TEST_NET_PATH else DerivationPath.MAIN_NET_PATH
            val accountName = CryptoUtils.prepareName(network.name, index)
            cryptographyRepository.calculateDerivedKeysSingle(
                walletManager.masterSeed.seed,
                walletManager.masterSeed.password,
                index,
                derivationPath,
                network.testNet
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
            }.flatMapCompletable { config -> walletManager.updateWalletConfig(config) }
                .toSingleDefault(accountName)
        }

    private fun addAccount(newAccount: Account, config: WalletConfig): WalletConfig {
        val newAccounts = config.accounts.toMutableList()
        newAccounts.add(config.accounts.size, newAccount)
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    private fun getWalletConfigWithNewAccounts(
        accounts: List<Account>,
        config: WalletConfig
    ): WalletConfig {
        val newAccounts = config.accounts.toMutableList().apply { addAll(accounts) }
        return config.copy(version = config.updateVersion, accounts = newAccounts)
    }

    override fun connectAccountToNetwork(index: Int, network: Network): Single<String> {
        val existedAccount =
            walletManager.getWalletConfig().accounts.filter { account -> account.id == index && account.isTestNetwork == network.testNet }
                .find { account -> account.chainId == Int.InvalidValue || (account.chainId == network.chainId && account.isHide) }
        return when {
            existedAccount != null -> updateAccount(existedAccount, network)
            else -> createRegularAccountWithGivenIndex(index, network)
        }
    }

    override fun connectAccountsToNetworks(networksListWithIdexes: List<Pair<Int, Network>>): Single<List<String>> {
        val networksList: MutableList<Pair<Account, Network>> = mutableListOf()
        networksListWithIdexes.forEach { pair ->
            val index = pair.first
            val network = pair.second
            val existedAccount: Account? =
                walletManager.getWalletConfig().accounts.filter { account -> account.id == index && account.isTestNetwork == network.testNet }
                    .find { account -> account.chainId == Int.InvalidValue || (account.chainId == network.chainId && account.isHide) }

            if (null != existedAccount) {
                networksList.add(Pair(existedAccount, network))
            }
        }
        return if (networksList.isEmpty()) {
            Single.just(listOf(EMPTY_LIST))
        } else {
            updateAccounts(networksList)
        }
    }

    override fun createOrUnhideAccount(network: Network): Single<String> {
        val index = getNextAvailableIndexForNetwork(network)
        return connectAccountToNetwork(index, network)
    }

    override fun createAccounts(networks: List<Network>): Single<List<String>> {
        //networks and them index(if exists) on main wallet accounts list
        var networksListWithIndexes: MutableList<Pair<Int, Network>> = mutableListOf()
        networks.forEach { network ->
            val index = getNextAvailableIndexForNetwork(network)
            networksListWithIndexes.add(Pair(index, network))
        }
        return connectAccountsToNetworks(networksListWithIndexes)
    }

    override fun insertCoinBalance(coinBalance: CoinBalance): Completable =
        with(coinBalance) {
            coinBalanceDao.insert(
                CoinBalanceEntity(
                    balanceHash = TokenUtils.generateTokenHash(chainId, address),
                    address = address,
                    chainId = chainId,
                    cryptoBalance = coinBalance.balance.cryptoBalance,
                    fiatBalance = coinBalance.balance.fiatBalance,
                    rate = rate ?: Double.InvalidValue
                )
            ).doOnComplete {
                _balancesInsertLiveData.postValue(Event(Unit))
            }
        }

    override fun insertTokenBalance(coinBalance: CoinBalance, accountAddress: String): Completable =
        with(coinBalance) {
            tokenBalanceDao.insert(
                TokenBalanceEntity(
                    balanceHash = (address + accountAddress).lowercase(Locale.ROOT),
                    address = address,
                    chainId = chainId,
                    cryptoBalance = coinBalance.balance.cryptoBalance,
                    fiatBalance = coinBalance.balance.fiatBalance,
                    rate = rate ?: Double.InvalidValue,
                    accountAddress = accountAddress
                )
            ).doOnComplete {
                _balancesInsertLiveData.postValue(Event(Unit))
            }
        }

    override fun getCachedCoinBalance(address: String, chainId: Int): Single<CoinBalance> =
        coinBalanceDao.getCoinBalance(address, chainId)
            .map { entity ->
                with(entity) {
                    CoinBalance(chainId, address, Balance(cryptoBalance, fiatBalance), rate)
                }
            }

    override fun getCachedTokenBalance(
        address: String,
        accountAddress: String
    ): Single<CoinBalance> =
        tokenBalanceDao.getTokenBalance(address, accountAddress)
            .map { entity ->
                with(entity) {
                    CoinBalance(chainId, address, Balance(cryptoBalance, fiatBalance), rate)
                }
            }

    /**
     * Update Accounts - updated/added main wallet accounts
     * @param networksList - networks which accounts have to be created
     * @return RXJava.Single - list with names of created accounts
     */
    private fun updateAccounts(networksList: MutableList<Pair<Account, Network>>): Single<List<String>> {
        var preparedAccountsList: List<Account> = mutableListOf()//list for main wallet accounts which will be updated
        val addedAccountsNames: MutableList<String> = mutableListOf()//list for accounts names which will be added
        walletManager.getWalletConfig().run {
            networksList.forEach { pair ->
                val existedAccount: Account = pair.first.copy()
                val network: Network = pair.second.copy()
                val accountName: String = CryptoUtils.prepareName( network.name, existedAccount.id )//create account name
                preparedAccountsList = if (existedAccount.chainId == Int.InvalidValue) {
                    if (preparedAccountsList.isEmpty()) {//add empty account to main wallet accounts
                        changeAccountIndexToLast(accounts.toMutableList(), existedAccount)//first iteration
                    } else {
                        changeAccountIndexToLast(preparedAccountsList.toMutableList(), existedAccount)
                    }
                } else accounts
                val existAccountIndex = preparedAccountsList.indexOf(existedAccount)//get index of added account
                preparedAccountsList[existAccountIndex].apply {//set specified network data to added account
                    name = accountName
                    chainId = network.chainId
                    isHide = false
                }
                addedAccountsNames.add(accountName)//add name of added account
            }

            return walletManager.updateWalletConfig(//update main wallet accounts
                copy(
                    version = updateVersion,
                    accounts = preparedAccountsList
                )
            )
                .toSingleDefault(addedAccountsNames)//return list names of added accounts
        }
    }

    private fun updateAccount(existedAccount: Account, network: Network): Single<String> {
        val accountName =
            existedAccount.name.ifBlank {
                CryptoUtils.prepareName(
                    network.name,
                    existedAccount.id
                )
            }
        walletManager.getWalletConfig().run {
            val newAccountsList = if (existedAccount.chainId == Int.InvalidValue) {
                changeAccountIndexToLast(accounts.toMutableList(), existedAccount)
            } else accounts
            val existAccountIndex = newAccountsList.indexOf(existedAccount)
            newAccountsList[existAccountIndex].apply {
                name = accountName
                chainId = network.chainId
                isHide = false
            }
            return walletManager.updateWalletConfig(
                copy(
                    version = updateVersion,
                    accounts = newAccountsList
                )
            )
                .toSingleDefault(accountName)
        }
    }

    private fun changeAccountIndexToLast(
        accountsList: MutableList<Account>,
        account: Account
    ): List<Account> =
        accountsList.toMutableList().apply {
            remove(account)
            add(account)
        }

    override fun changeShowWarning(existedAccount: Account, state: Boolean): Completable {
        walletManager.getWalletConfig().run {
            accounts.find { account -> account.id == existedAccount.id && account.chainId == existedAccount.chainId }
                ?.apply {
                    showWarning = state
                }
            return walletManager.updateWalletConfig(
                copy(
                    version = updateVersion,
                    accounts = accounts
                )
            )
        }
    }

    override fun changeFavoriteState(existedAccount: Account, tokenId: String, isFavoriteState: Boolean): Completable {
        walletManager.getWalletConfig().run {
            accounts.find { account -> account.id == existedAccount.id && account.chainId == existedAccount.chainId }?.apply {
                accountTokens.find { it.token.tokenId == tokenId }?.apply {
                    token.isFavorite = isFavoriteState
                }
            }
            erc20Tokens.values.forEach { erc20TokenList ->
                erc20TokenList.find { it.tokenId == tokenId }?.apply {
                    isFavorite = isFavoriteState
                }
            }
            return walletManager.updateWalletConfig(
                copy(
                    version = updateVersion,
                    accounts = accounts
                )
            )
        }
    }

    override fun changeAccountName(existedAccount: Account, newName: String): Completable {
        val accountName = CryptoUtils.prepareName(newName, existedAccount.id)
        walletManager.getWalletConfig().run {
            accounts.find { account -> account.id == existedAccount.id && account.chainId == existedAccount.chainId }
                ?.apply {
                    name = accountName
                }
            return walletManager.updateWalletConfig(
                copy(
                    version = updateVersion,
                    accounts = accounts
                )
            )
        }
    }

    override fun createSafeAccount(account: Account, contract: String): Completable =
        walletManager.getWalletConfig().run {
            val (index, derivationPath) = getIndexWithDerivationPath(account.network.testNet, this)
            return cryptographyRepository.calculateDerivedKeysSingle(
                walletManager.masterSeed.seed,
                walletManager.masterSeed.password,
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

    internal fun filterCachedTokens(tokenMap: Map<Int, List<ERCToken>>): Map<Int, List<ERCToken>> {
        val newMap: MutableMap<Int, MutableList<ERCToken>> = mutableMapOf()
        activeAccounts.filter { account -> !account.isPending }.forEach { account ->
            if (newMap.containsKey(account.chainId)) {
                newMap[account.chainId]?.addAll(getVisibleTokensList(tokenMap, account))
            } else {
                newMap[account.chainId] = getVisibleTokensList(tokenMap, account).toMutableList()
            }
        }
        return newMap
    }

    private fun getVisibleTokensList(
        tokenMap: Map<Int, List<ERCToken>>,
        account: Account
    ): List<ERCToken> =
        tokenMap[account.chainId]?.let { tokens ->
            tokens.filter { token ->
                token.accountAddress.equals(account.address, true) &&
                        getTokenVisibilitySettings.getTokenVisibility(
                            account.address,
                            token.address
                        ) == true
            }
        } ?: emptyList()

    private fun getIndexWithDerivationPath(
        isTestNetwork: Boolean,
        config: WalletConfig
    ): Pair<Int, String> =
        if (isTestNetwork) {
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

    override fun isAddressValid(address: String): Boolean = validationRepository.isAddressValid(address)

    override fun saveFreeATSTimestamp() {
        localStorage.saveFreeATSTimestamp(timeProvider.currentTimeMills())
    }

    override fun getLastFreeATSTimestamp(): Long = localStorage.loadLastFreeATSTimestamp()

    override fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings =
        localStorage.saveTokenVisibilitySettings(settings)

    override fun currentTimeMills(): Long = timeProvider.currentTimeMills()

    override fun getAllAccounts(): List<Account> = walletManager.getWalletConfig().accounts

    override fun getAllActiveAccounts(chainId: Int): List<Account> =
        getAllAccounts().filter { account -> !account.isHide && !account.isDeleted && account.chainId == chainId }

    override fun getFirstActiveAccountForAllNetworks(): List<Account> =
        getAllAccountsForSelectedNetworksType().filter { account -> account.shouldShow }
            .distinctBy { account -> account.chainId }

    override fun getFirstActiveAccountOrNull(chainId: Int): Account? =
        getAllActiveAccounts(chainId).firstOrNull()

    override fun toChecksumAddress(address: String, chainId: Int?): String =
        validationRepository.toChecksumAddress(address, chainId)

    override fun getAllAccountsForSelectedNetworksType(): List<Account> =
        getAllAccounts().filter { account -> account.isTestNetwork == !areMainNetworksEnabled }

    override fun getAllFreeAccountForNetwork(chainId: Int): List<AddressWrapper> {
        //get all accounts
        val accounts =
            getAllAccounts().filter { !it.isDeleted && it.isTestNetwork == !areMainNetworksEnabled }
        //filtering accounts which chain already uses
        val usedAccounts: List<Account> = accounts.filter { it.chainId == chainId }
        //id of account which chain already uses to integer list
        val usedIdsOfAccounts: List<Int> = usedAccounts.map { account -> account.id }
        //filtering available addresses for chain
        val availableAccounts: MutableList<Account> = accounts
            .filter { !usedIdsOfAccounts.contains(it.id) }
            .distinctBy { it.id }
            .toMutableList()
        //replace same addresses from other chain - for get correct state af address
        usedAccounts.forEach { used ->
            val equalAddressHashFromDiffChain: Account? =
                availableAccounts.find { used.address == it.address }
            if (null != equalAddressHashFromDiffChain) {
                availableAccounts.remove(equalAddressHashFromDiffChain)
            }
        }
        //merge available and used addresses
        val allChainAccounts: Set<Account> = availableAccounts.union(usedAccounts)
        //wrapping addresses to AddressWrapper for put it forward

        return allChainAccounts
            .map { account -> accountToAddressWrapper(account, usedIdsOfAccounts) }
            .sortedBy { account -> account.index }
    }

    /**
     * Account To Address Wrapper -  wrapping specified account to AddressWrapper
     * @param account - current account
     * @param usedIds - ids which account already used
     * @return AddressWrapper with correct states
     */
    private fun accountToAddressWrapper(account: Account, usedIds: List<Int> = listOf()): AddressWrapper {
        val addressStatus: AddressStatus = if (!usedIds.contains(account.id)) { //acc didn't use this address earlier
            AddressStatus.FREE
        } else {
            if (account.isHide) {
                AddressStatus.HIDDEN
            } else {
                AddressStatus.ALREADY_IN_USE
            }
        }
        return AddressWrapper(account.id, account.address, addressStatus)
    }

    override fun getNumberOfAccountsToUse() =
        getAllAccountsForSelectedNetworksType().filter { account -> !account.isDeleted }
            .distinctBy { account -> account.id }.size

    override fun clearFiat() =
        walletManager.getWalletConfig().accounts.forEach { account ->
            account.fiatBalance = Double.InvalidValue.toBigDecimal()
            account.accountTokens.forEach { accountToken ->
                accountToken.tokenPrice = Double.InvalidValue
            }
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
        item: Account,
        config: WalletConfig,
        newAccounts: MutableList<Account>,
        index: Int
    )
            : Completable =
        when {
            areFundsOnValue(item.cryptoBalance, item.accountTokens) -> handleNoFundsError(item)
            isNotSAMasterOwner(config.accounts, item) -> Completable.error(
                IsNotSafeAccountMasterOwnerThrowable()
            )
            else -> {
                newAccounts[index] = item.copy(isDeleted = true)
                walletManager.updateWalletConfig(
                    config.copy(
                        version = config.updateVersion,
                        accounts = newAccounts
                    )
                )
            }
        }

    override fun hideAccount(account: Account): Completable =
        walletManager.getWalletConfig().run {
            val newAccounts: MutableList<Account> = accounts.toMutableList()
            val accountsToChange: MutableMap<Int, Account> = mutableMapOf()
            accounts.forEachIndexed { index, item ->
                if (item.id == account.id && item.network == account.network) {
                    accountsToChange[index] = item
                }
            }
            if (accountsToChange.isNotEmpty()) {
                handleHidingAccount(this, newAccounts, accountsToChange)
            } else {
                Completable.error(MissingAccountThrowable())
            }
        }

    private fun handleHidingAccount(
        config: WalletConfig,
        newAccounts: MutableList<Account>, accountsToChange: Map<Int, Account>
    ): Completable {
        accountsToChange.entries.forEach { item ->
            when {
                isNotSAMasterOwner(config.accounts, item.value) -> return Completable.error(
                    IsNotSafeAccountMasterOwnerThrowable()
                )
                else -> {
                    newAccounts[item.key] = item.value.copy(isHide = true)

                }
            }
        }
        return walletManager.updateWalletConfig(
            config.copy(
                version = config.updateVersion,
                accounts = newAccounts
            )
        )
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
        return unitConverter.toGwei(balance).toBigInteger() >= MAX_GWEI_TO_REMOVE_VALUE
    }

    override fun getSafeAccountCount(ownerAddress: String): Int =
        if (ownerAddress == String.Empty) NO_SAFE_ACCOUNTS
        else walletManager.getSafeAccountNumber(ownerAddress)

    override fun loadAccount(index: Int): Account = walletManager.getWalletConfig().accounts.run {
        if (inBounds(index)) {
            val account = this[index]
            account.copy(address = toChecksumAddress(account.address, account.chainId))
        } else Account(Int.InvalidIndex)
    }

    companion object {
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private const val NO_SAFE_ACCOUNTS = 0
        private const val EMPTY_LIST = "empty_list"
    }
}