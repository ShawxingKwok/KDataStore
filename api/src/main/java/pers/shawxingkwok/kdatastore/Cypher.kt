package pers.shawxingkwok.kdatastore

import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

public interface Cypher {
    public fun encrypt(data: ByteArray): ByteArray
    public fun decrypt(data: ByteArray): ByteArray

    /**
     * Persist [ivBytes], [password] and [salt] somewhere if you generate them randomly.
     * @param ivBytes size must be 16
     * @param keyLength must be 128, 192 or 256
     */
    public class AES (
        password: String,
        salt: String,
        ivBytes: ByteArray,
        algorithm: String = "AES/CBC/PKCS5Padding",
        keyAlgorithm: String = "PBKDF2WithHmacSHA256",
        iterationTimes: Int = 65536,
        keyLength: Int = 256,
    ) : Cypher {
        init {
            require(
                keyLength == 128
                || keyLength == 192
                || keyLength == 256
            ) {
                "keyLength must be 128, 192 or 256."
            }

            require(ivBytes.size == 16) {
                "ivBytes length must be 16."
            }
        }

        private val iv: IvParameterSpec = IvParameterSpec(ivBytes)

        private val key: SecretKey = run {
            val factory: SecretKeyFactory = SecretKeyFactory.getInstance(keyAlgorithm)
            val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterationTimes, keyLength)
            val key = factory.generateSecret(spec).encoded
            SecretKeySpec(key, "AES")
        }

        private val encryptCipher = Cipher.getInstance(algorithm).also {
            it.init(Cipher.ENCRYPT_MODE, key, iv)
        }

        private val decryptCipher = Cipher.getInstance(algorithm).also {
            it.init(Cipher.DECRYPT_MODE, key, iv)
        }

        override fun encrypt(data: ByteArray): ByteArray = encryptCipher.doFinal(data)

        override fun decrypt(data: ByteArray): ByteArray = decryptCipher.doFinal(data)
    }
}