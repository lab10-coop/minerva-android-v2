package minerva.android.walletmanager.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreRepositoryImpl(private val context: Context): KeystoreRepository {

    override fun isMasterKeySaved(): Boolean {
        val keyStore = KeyStore.getInstance(DIR_PROVIDER).apply { load(null) }
        val secretKeyEntry = keyStore.getEntry(MINERVA_ALIAS, null) as? KeyStore.SecretKeyEntry
        return secretKeyEntry?.let {
            true
        } ?: false
    }

    override fun encryptKey(masterKey: MasterKey) {
        val rawMasterKey = Gson().toJson(masterKey)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey())
        saveMasterKeyToSharedPrefs(cipher.doFinal(rawMasterKey.toByteArray()), cipher.iv)
    }

    override fun decryptKey(): MasterKey {
        if (!isMasterKeySaved()) throw IllegalStateException("Decrypt Error: No Master Key saved!")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, getEncryptedData(INIT_VECTOR))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decoded = cipher.doFinal(getEncryptedData(MASTER_KEY))
        val rawMasterKey = String(decoded, Charsets.UTF_8)
        return Gson().fromJson(rawMasterKey, MasterKey::class.java)
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(DIR_PROVIDER).apply { load(null) }
        val secretKeyEntry = keyStore.getEntry(MINERVA_ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    private fun saveMasterKeyToSharedPrefs(encryptedMasterKey: ByteArray, initVector: ByteArray) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(MASTER_KEY, Base64.encodeToString(encryptedMasterKey, Base64.DEFAULT))
            putString(INIT_VECTOR, Base64.encodeToString(initVector, Base64.DEFAULT))
            apply()
        }
    }

    private fun getEncryptedData(value: String): ByteArray =
        Base64.decode(getSharedPrefsData(value), Base64.DEFAULT)

    private fun getSharedPrefsData(value: String): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(value, String.NO_DATA) ?: String.NO_DATA

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, DIR_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec
                .Builder(MINERVA_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    companion object {
        private const val DIR_PROVIDER = "AndroidKeyStore"
        private const val MINERVA_ALIAS = "MinervaAlias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AUTHENTICATION_TAG_LENGTH = 128
        private const val PREFS_NAME = "MinervaSharedPrefs"
        private const val MASTER_KEY = "MasterKey"
        private const val INIT_VECTOR = "InitializationVector"
    }
}