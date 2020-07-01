package minerva.android.walletmanager.keystore

import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.MasterSeed
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class KeystoreRepositoryImpl(private val sharedPreferences: SharedPreferences, private val keyStoreManager: KeyStoreManager) :
    KeystoreRepository {

    override fun isMasterSeedSaved(): Boolean = keyStoreManager.isMasterSeedSaved()

    override fun encryptMasterSeed(masterSeed: MasterSeed) {
        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, keyStoreManager.generateSecretKey())
            saveMasterSeedToSharedPrefs(doFinal(Gson().toJson(masterSeed).toByteArray()), iv)
        }
    }

    override fun decryptMasterSeed(): MasterSeed? {
        if (!isMasterSeedSaved()) return null
        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, keyStoreManager.getSecretKey(), getGcmSpec())
            return Gson().fromJson(String(doFinal(getEncryptedData(MASTER_SEED)), Charsets.UTF_8), MasterSeed::class.java)
        }
    }

    private fun getGcmSpec() = GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, getEncryptedData(INIT_VECTOR))

    private fun saveMasterSeedToSharedPrefs(encryptedMasterSeed: ByteArray, initVector: ByteArray) {
        sharedPreferences.edit().apply {
            putString(MASTER_SEED, Base64.encodeToString(encryptedMasterSeed, Base64.DEFAULT)).apply()
            putString(INIT_VECTOR, Base64.encodeToString(initVector, Base64.DEFAULT)).apply()
        }
    }

    private fun getEncryptedData(value: String): ByteArray = Base64.decode(getSharedPrefsData(value), Base64.DEFAULT)

    private fun getSharedPrefsData(value: String): String = sharedPreferences.getString(value, String.NO_DATA) ?: String.NO_DATA

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AUTHENTICATION_TAG_LENGTH = 128
        private const val MASTER_SEED = "MasterSeed"
        private const val INIT_VECTOR = "InitializationVector"
    }
}