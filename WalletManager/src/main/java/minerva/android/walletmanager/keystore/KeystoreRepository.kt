package minerva.android.walletmanager.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.lang.IllegalStateException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreRepository(private val context: Context) {

    fun isMasterKeySaved(): Boolean =
        getSharedPrefsData(context, INIT_VECTOR) != EMPTY_DATA && getSharedPrefsData(context, MASTER_KEY) != EMPTY_DATA

    fun encryptKey(masterKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey())
        saveMasterKeyToSharedPrefs(context, cipher.doFinal(masterKey.toByteArray()), cipher.iv)
    }

    fun decryptKey(): String {
        if(!isMasterKeySaved()) throw IllegalStateException("Decrypt Error: No Master Key saved!")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, getEncryptedData(context, INIT_VECTOR))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decoded = cipher.doFinal(getEncryptedData(context, MASTER_KEY))
        return String(decoded, Charsets.UTF_8)
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(DIR_PROVIDER).apply { load(null) }
        val secretKeyEntry = keyStore.getEntry(MINERVA_ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    private fun saveMasterKeyToSharedPrefs(context: Context, encryptedMasterKey: ByteArray, initVector: ByteArray) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(MASTER_KEY, Base64.encodeToString(encryptedMasterKey, Base64.DEFAULT))
            putString(INIT_VECTOR, Base64.encodeToString(initVector, Base64.DEFAULT))
            apply()
        }
    }

    private fun getEncryptedData(context: Context, value: String): ByteArray =
        Base64.decode(getSharedPrefsData(context, value), Base64.DEFAULT)

    private fun getSharedPrefsData(context: Context, value: String): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(value, EMPTY_DATA) ?: EMPTY_DATA

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
        private const val EMPTY_DATA = ""
    }
}