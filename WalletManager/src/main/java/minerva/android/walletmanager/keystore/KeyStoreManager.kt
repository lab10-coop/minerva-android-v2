package minerva.android.walletmanager.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager {

    fun isMasterSeedSaved(): Boolean {
        val secretKeyEntry =
            KeyStore.getInstance(DIR_PROVIDER)
                .apply { load(null) }
                .getEntry(MINERVA_ALIAS, null) as? KeyStore.SecretKeyEntry
        return secretKeyEntry?.let { true } ?: false

    }

    fun generateSecretKey(): SecretKey {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, DIR_PROVIDER).run {
            init(
                KeyGenParameterSpec
                    .Builder(MINERVA_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            return generateKey()
        }
    }

    fun getSecretKey(): SecretKey =
        (KeyStore.getInstance(DIR_PROVIDER)
            .apply { load(null) }
            .getEntry(MINERVA_ALIAS, null) as KeyStore.SecretKeyEntry)
            .secretKey

    companion object {
        private const val DIR_PROVIDER = "AndroidKeyStore"
        private const val MINERVA_ALIAS = "MinervaAlias"
    }
}