package pers.shawxingkwok.kdatastore.demo.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.androidutil.AppContext
import pers.shawxingkwok.androidutil.KLog
import pers.shawxingkwok.kdatastore.KDataStore
import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val cipher = run {
    val fixedSecretKey = "0123456789abcdef0123456789abcdef" // 32 characters for AES-256
    val key = SecretKeySpec(fixedSecretKey.toByteArray(StandardCharsets.UTF_8), "AES") // 32 bytes key for AES-256
    val iv = IvParameterSpec(ByteArray(16)) // 16 bytes for the initialization vector

    // Encrypt the plaintext with AES
    val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv)

    // Decrypt the encrypted text
    val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    decryptCipher.init(Cipher.DECRYPT_MODE, key, iv)

    object : pers.shawxingkwok.kdatastore.Cipher{
        override fun encrypt(bytes: ByteArray): ByteArray {
            return encryptCipher.doFinal(bytes)
        }

        override fun decrypt(bytes: ByteArray): ByteArray {
            return decryptCipher.doFinal(bytes)
        }
    }
}

/**
 * - removing needless keys /
 */
@OptIn(KDataStore.CautiousApi::class)
@RunWith(AndroidJUnit4::class)
class KDataStoreTest : KDataStore("kdatastoreTest", cipher = cipher) {
    val _i by storeNullable<Int>()
    val location by store(Location(1.2f, 0.0f))
    val _location by storeNullable<Location>()

    val l by store<Location, FloatArray>(
        Location(1.2f, 0.0f),
        convert = {
            floatArrayOf(it.latitude, it.longitude)
        },
        recover = {
            Location(it.first(), it[1])
        }
    )

    val _l by storeNullable<Location, FloatArray>(
        convert = {
            floatArrayOf(it.latitude, it.longitude)
        },
        recover = {
            Location(it.first(), it[1])
        }
    )

    @Test
    fun start(){
        KLog.d(_i.value)
        KLog.d(location.value)
        KLog.d(_location.value)
        KLog.d(l.value)
        KLog.d(_l.value)

        _i.update { it?.plus(1) ?: 1  }
        location.update { it.copy(longitude = it.longitude + 1) }
        _location.update { it?.copy(longitude = it.longitude + 1) ?: Location(0.0f, 0.0f)}
        l.update { it.copy(longitude = it.longitude + 1) }
        _l.update { it?.copy(longitude = it.longitude + 1) ?: Location(0.0f, 0.0f)}

        runBlocking { delay(1000) }

        // File(AppContext.filesDir, "datastore/kdatastoreTest.preferences_pb")
        //     .writeText("Fd")
        //
        // File(AppContext.filesDir, "datastore/kdatastoreTest.bak.preferences_pb")
        //     .writeText("Fd")
    }
}