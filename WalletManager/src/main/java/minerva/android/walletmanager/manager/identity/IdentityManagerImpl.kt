package minerva.android.walletmanager.manager.identity

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.exception.CannotRemoveLastIdentityThrowable
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.exception.NoIdentityToRemoveThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.utils.DateUtils

class IdentityManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository
) : IdentityManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun saveIdentity(identity: Identity): Completable {
        with(walletConfigManager) {
            getWalletConfig()?.let {
                return cryptographyRepository.computeDeliveredKeys(masterSeed.seed, identity.index)
                    .map { keys ->
                        WalletConfig(
                            it.updateVersion,
                            prepareIdentities(getIdentity(identity, keys), it),
                            it.accounts,
                            it.services
                        )
                    }
                    .flatMapCompletable { updateWalletConfig(it) }
            }
            throw NotInitializedWalletConfigThrowable()
        }
    }

    private fun getIdentity(identity: Identity, keys: DerivedKeys): Identity =
        identity.apply {
            publicKey = keys.publicKey
            privateKey = keys.privateKey
            address = keys.address
        }

    override fun loadIdentity(position: Int, defaultName: String): Identity {
        walletConfigManager.getWalletConfig()?.identities?.apply {
            return if (inBounds(position)) this[position]
            else getDefaultIdentity(defaultName)
        }
        return getDefaultIdentity(defaultName)
    }

    private fun getDefaultIdentity(defaultName: String) = Identity(getNewIndex(), prepareDefaultIdentityName(defaultName))

    private fun prepareDefaultIdentityName(defaultName: String): String =
        String.format(NEW_IDENTITY_TITLE_PATTERN, defaultName, getNewIndex())

    private fun getNewIndex(): Int {
        walletConfigManager.getWalletConfig()?.let { return it.newIndex }
        return Int.InvalidIndex
    }

    override fun removeIdentity(identity: Identity): Completable {
        walletConfigManager.getWalletConfig()?.let {
            return handleRemovingIdentity(it.identities, getPositionForIdentity(identity, it), identity)
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun handleRemovingIdentity(identities: List<Identity>, currentPosition: Int, identity: Identity): Completable {
        if (!identities.inBounds(currentPosition)) return Completable.error(NoIdentityToRemoveThrowable())
        if (isOnlyOneElement(identities)) return Completable.error(CannotRemoveLastIdentityThrowable())
        return saveIdentity(
            Identity(
                identity.index,
                identity.name,
                identity.publicKey,
                identity.privateKey,
                identity.address,
                identity.personalData,
                true,
                identity.credentials
            )
        )
    }

    private fun isOnlyOneElement(identities: List<Identity>): Boolean {
        var realIdentitiesCount = 0
        identities.forEach {
            if (!it.isDeleted) realIdentitiesCount++
        }
        return realIdentitiesCount <= ONE_ELEMENT
    }

    override fun bindCredentialToIdentity(newCredential: Credential): Single<String> {
        walletConfigManager.getWalletConfig()?.let {
            return bindCredential(newCredential, it)
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun bindCredential(newCredential: Credential, walletConfig: WalletConfig): Single<String> {
        getLoggedInIdentity(newCredential.loggedInIdentityDid)?.let { identity ->
            return updateWalletConfig(getIdentityWithNewCredential(identity, newCredential), walletConfig).toSingleDefault(identity.name)
        }
        return Single.error(NoBindedCredentialThrowable())
    }

    override fun updateBindedCredential(credential: Credential): Single<String> {
        walletConfigManager.getWalletConfig()?.apply {
            getLoggedInIdentity(credential.loggedInIdentityDid)?.let {
                getBindedCredential(it, credential)?.let { credential ->
                    updateCredential(it, credential)
                    return updateWalletConfig(it, this).toSingleDefault(it.name)
                }
            }
            return Single.error(NoBindedCredentialThrowable())
        }
        throw  NotInitializedWalletConfigThrowable()
    }


    override fun removeBindedCredentialFromIdentity(credential: Credential): Completable {
        walletConfigManager.getWalletConfig()?.let {
            getLoggedInIdentity(credential.loggedInIdentityDid)?.let { identity ->
                val newCredentials = identity.credentials.toMutableList()
                newCredentials.remove(credential)
                identity.credentials = newCredentials
                return updateWalletConfig(identity, it)
            }
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    override fun isCredentialBinded(loggedInIdentityDid: String, issuer: String): Boolean =
        getLoggedInIdentity(loggedInIdentityDid)?.credentials?.find { it.issuer == issuer } != null

    private fun getLoggedInIdentity(loggedInIdentityDid: String): Identity? =
        walletConfigManager.getWalletConfig()?.identities?.filter { !it.isDeleted }?.find { it.did == loggedInIdentityDid }

    private fun getBindedCredential(identity: Identity, credential: Credential): Credential? =
        identity.credentials.find { item -> item.issuer == credential.issuer && item.type == credential.type }

    private fun updateCredential(identity: Identity, credential: Credential) {
        identity.credentials.find { found -> found == credential }?.lastUsed = DateUtils.timestamp
    }

    private fun getIdentityWithNewCredential(identity: Identity, newCredential: Credential): Identity =
        identity.apply { credentials = credentials + newCredential }

    private fun updateWalletConfig(identity: Identity, walletConfig: WalletConfig): Completable =
        walletConfigManager.updateWalletConfig(
            WalletConfig(
                walletConfig.version,
                prepareIdentities(identity, walletConfig),
                walletConfig.accounts,
                walletConfig.services
            )
        )

    private fun prepareIdentities(identity: Identity, walletConfig: WalletConfig): List<Identity> {
        val position = getPositionForIdentity(identity, walletConfig)
        walletConfig.identities.toMutableList().apply {
            if (inBounds(position)) this[position] = identity
            else add(identity)
            return this
        }
    }

    private fun getPositionForIdentity(newIdentity: Identity, walletConfig: WalletConfig): Int {
        walletConfig.identities.forEachIndexed { position, identity ->
            if (newIdentity.index == identity.index) return position
        }
        return walletConfig.identities.size
    }

    companion object {
        private const val ONE_ELEMENT = 1
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
    }
}