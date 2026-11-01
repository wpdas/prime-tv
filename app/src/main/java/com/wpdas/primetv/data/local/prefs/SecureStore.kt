package com.wpdas.primetv.data.local.prefs

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Criptografia local com Tink (AES-256-GCM). O keyset fica em SharedPreferences,
 * protegido por uma chave mestra no Android Keystore. Usado para a URL da lista,
 * que carrega usuário e senha.
 */
@Singleton
class SecureStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun encrypt(plain: String): String =
        Base64.encodeToString(aead.encrypt(plain.toByteArray(Charsets.UTF_8), AAD), Base64.NO_WRAP)

    /** null se o conteúdo não puder ser decifrado (keyset perdido, dado corrompido). */
    fun decrypt(cipher: String): String? = runCatching {
        String(aead.decrypt(Base64.decode(cipher, Base64.NO_WRAP), AAD), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val KEYSET_NAME = "primetv_keyset"
        const val PREF_FILE = "primetv_secure_prefs"
        const val MASTER_KEY_URI = "android-keystore://primetv_master_key"
        val AAD = "primetv".toByteArray()
    }
}
