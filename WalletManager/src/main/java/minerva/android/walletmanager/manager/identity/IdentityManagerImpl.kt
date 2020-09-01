package minerva.android.walletmanager.manager.identity

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.kotlinUtils.mapper.BitmapMapper
import minerva.android.walletmanager.exception.CannotRemoveLastIdentityThrowable
import minerva.android.walletmanager.exception.NoIdentityToRemoveThrowable
import minerva.android.walletmanager.exception.NoLoggedInIdentityThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.model.mappers.CredentialQrCodeToCredentialMapper

class IdentityManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val localStorage: LocalStorage
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
                            it.services,
                            it.credentials
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
                true
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

    override fun bindCredentialToIdentity(qrCode: CredentialQrCode): Single<String> {
        walletConfigManager.getWalletConfig()?.let {
            with(it) {
                return if (doesIdentityExist(qrCode.loggedInDid)) {
                    walletConfigManager.updateWalletConfig(
                        WalletConfig(
                            updateVersion,
                            identities,
                            accounts,
                            services,
                            credentials + CredentialQrCodeToCredentialMapper.map(qrCode)
                        )
                    ).toSingleDefault(findIdentityByDid(qrCode.loggedInDid)?.name)
                } else Single.error(NoLoggedInIdentityThrowable())
            }
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun doesIdentityExist(did: String): Boolean =
        findIdentityByDid(did) != null

    private fun findIdentityByDid(did: String) =
        walletConfigManager.getWalletConfig()?.let { config -> config.identities.find { it.did == did } }

    override fun updateBindedCredential(qrCode: CredentialQrCode): Single<String> {
        walletConfigManager.getWalletConfig()?.apply {
            val updatedCredential = CredentialQrCodeToCredentialMapper.map(qrCode)
            val newCredentials = credentials.toMutableList().apply {
                this[getPositionForCredential(updatedCredential)] = updatedCredential
            }
            return walletConfigManager.updateWalletConfig(WalletConfig(updateVersion, identities, accounts, services, newCredentials))
                .toSingleDefault(findIdentityByDid(qrCode.loggedInDid)?.name)
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    override fun isCredentialLoggedIn(qrCode: CredentialQrCode): Boolean {
        walletConfigManager.getWalletConfig()?.credentials?.let { credentials ->
            if (credentials.isNotEmpty()) {
                credentials.filter { !it.isDeleted }.forEach { credential -> return isCredentialBinded(qrCode, credential) }
            } else return false
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun isCredentialBinded(qrCode: CredentialQrCode, credential: Credential) =
        qrCode.loggedInDid == credential.loggedInIdentityDid && qrCode.type == credential.type && qrCode.issuer == credential.issuer

    override fun removeBindedCredentialFromIdentity(credential: Credential): Completable {
        walletConfigManager.getWalletConfig()?.apply {
            val newCredentials = credentials.toMutableList()
            newCredentials.remove(credential)
            return walletConfigManager.updateWalletConfig(WalletConfig(updateVersion, identities, accounts, services, newCredentials))
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun prepareIdentities(identity: Identity, walletConfig: WalletConfig): List<Identity> {
        val position = getPositionForIdentity(identity, walletConfig)
        identity.profileImageBitmap?.let {
            localStorage.saveProfileImage(identity.profileImageName, BitmapMapper.toBase64(it))
        }
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

    private fun WalletConfig.getPositionForCredential(credential: Credential): Int {
        credentials.forEachIndexed { position, item ->
            if (item.loggedInIdentityDid == credential.loggedInIdentityDid && item.type == credential.type && item.issuer == credential.issuer) return position
        }
        return identities.size
    }

    companion object {
        private const val ONE_ELEMENT = 1
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
    }
}