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
import minerva.android.walletmanager.model.mappers.CredentialQrCodeToCredentialMapper
import minerva.android.walletmanager.storage.LocalStorage

class IdentityManagerImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository,
    private val localStorage: LocalStorage
) : IdentityManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun saveIdentity(identity: Identity): Completable {
        walletConfigManager.getWalletConfig()?.let {
            return cryptographyRepository.computeDeliveredKeys(walletConfigManager.masterSeed.seed, identity.index)
                .map { keys -> it.copy(version = it.updateVersion, identities = prepareIdentities(getIdentity(identity, keys), it)) }
                .flatMapCompletable { walletConfigManager.updateWalletConfig(it) }
        }
        throw NotInitializedWalletConfigThrowable()
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

    override fun loadIdentityByDID(did: String): Identity =
        walletConfigManager.findIdentityByDid(did) ?: Identity(index = Int.InvalidIndex)

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
            return if (doesIdentityExist(qrCode.loggedInDid)) {
                walletConfigManager.updateWalletConfig(
                    it.copy(
                        version = it.updateVersion,
                        credentials = it.credentials + CredentialQrCodeToCredentialMapper.map(qrCode)
                    )
                ).toSingleDefault(walletConfigManager.findIdentityByDid(qrCode.loggedInDid)?.name)
            } else Single.error(NoLoggedInIdentityThrowable())
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun doesIdentityExist(did: String): Boolean = walletConfigManager.findIdentityByDid(did) != null

    override fun isCredentialLoggedIn(qrCode: CredentialQrCode): Boolean {
        walletConfigManager.getWalletConfig()?.credentials?.let { credentials ->
            credentials.filter { !it.isDeleted }.forEach { credential -> if (isCredentialBinded(qrCode, credential)) return true }
            return false
        }
        throw  NotInitializedWalletConfigThrowable()
    }

    private fun isCredentialBinded(qrCode: CredentialQrCode, credential: Credential) =
        qrCode.issuer == credential.issuer && qrCode.type.type == credential.type && qrCode.loggedInDid == credential.loggedInIdentityDid

    override fun removeBindedCredentialFromIdentity(credential: Credential): Completable {
        walletConfigManager.getWalletConfig()?.apply {
            val newCredentials = credentials.toMutableList()
            newCredentials.remove(credential)
            return walletConfigManager.updateWalletConfig(copy(version = updateVersion, credentials = newCredentials))
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

    companion object {
        private const val ONE_ELEMENT = 1
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
    }
}