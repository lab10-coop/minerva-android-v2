package minerva.android.walletmanager.manager.values

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
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import java.math.BigDecimal
import java.math.BigInteger

class ValueManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRepository
) : ValueManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun createValue(network: Network, valueName: String, ownerAddress: String, contract: String): Completable {
        with(walletConfigManager) {
            getWalletConfig()?.let { config ->
                val newValue = Value(config.newIndex, name = valueName, network = network.short, bindedOwner = ownerAddress)
                return cryptographyRepository.computeDeliveredKeys(masterSeed.seed, newValue.index)
                    .map { createUpdatedWalletConfig(config, newValue, it, ownerAddress, contract) }
                    .flatMapCompletable { updateWalletConfig(it) }
            }
            return Completable.error(Throwable("Wallet Config was not initialized"))
        }
    }

    private fun createUpdatedWalletConfig(
        config: WalletConfig, newValue: Value,
        derivedKeys: DerivedKeys, ownerAddress: String,
        contractAddress: String
    ): WalletConfig {
        prepareNewValue(newValue, derivedKeys, ownerAddress, contractAddress)
        config.run {
            val newValues = values.toMutableList()
            var newValuePosition = values.size
            values.forEachIndexed { position, value ->
                if (value.address == ownerAddress && ownerAddress != String.Empty)
                    newValuePosition = position + getSafeAccountCount(ownerAddress)
            }
            newValues.add(newValuePosition, newValue)
            return WalletConfig(updateVersion, identities, newValues, services)
        }
    }

    private fun prepareNewValue(newValue: Value, derivedKeys: DerivedKeys, ownerAddress: String, contractAddress: String) {
        newValue.apply {
            publicKey = derivedKeys.publicKey
            privateKey = derivedKeys.privateKey
            if (ownerAddress.isNotEmpty()) owners = mutableListOf(ownerAddress)
            address = if (contractAddress.isNotEmpty()) {
                this.contractAddress = contractAddress
                contractAddress
            } else derivedKeys.address
        }
    }

    override fun removeValue(index: Int): Completable {
        walletConfigManager.getWalletConfig()?.let {
            val newValues = it.values.toMutableList()
            it.values.forEachIndexed { position, value ->
                if (value.index == index) {
                    when {
                        areFundsOnValue(value.cryptoBalance, value.assets) || hasMoreOwners(value) ->
                            return Completable.error(BalanceIsNotEmptyAndHasMoreOwnersThrowable())
                        isNotSafeAccountMasterOwner(it.values, value) ->
                            return Completable.error(IsNotSafeAccountMasterOwnerThrowable())
                    }
                    newValues[position] = Value(value, true)
                    return walletConfigManager.updateWalletConfig(
                        WalletConfig(it.updateVersion, it.identities, newValues, it.services)
                    )
                }
            }
            return Completable.error(Throwable("Missing value with this index"))
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    private fun isNotSafeAccountMasterOwner(values: List<Value>, value: Value): Boolean {
        value.owners?.let {
            values.forEach { if (it.address == value.masterOwnerAddress) return false }
            return true
        }
        return false
    }

    private fun hasMoreOwners(value: Value): Boolean {
        value.owners?.let { return it.size > 1 }
        return false
    }

    private fun areFundsOnValue(balance: BigDecimal, assets: List<Asset>): Boolean {
        assets.forEach {
            if (blockchainRepository.toGwei(it.balance) >= MAX_GWEI_TO_REMOVE_VALUE) return true
        }
        return blockchainRepository.toGwei(balance) >= MAX_GWEI_TO_REMOVE_VALUE
    }

    override fun getSafeAccountCount(ownerAddress: String): Int =
        if (ownerAddress == String.Empty) NO_SAFE_ACCOUNTS
        else walletConfigManager.getSafeAccountNumber(ownerAddress)

    override fun loadValue(position: Int): Value {
        walletConfigManager.getWalletConfig()?.values?.apply {
            return if (inBounds(position)) this[position]
            else Value(Int.InvalidIndex)
        }
        return Value(Int.InvalidIndex)
    }

    companion object {
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private const val NO_SAFE_ACCOUNTS = 0
    }
}