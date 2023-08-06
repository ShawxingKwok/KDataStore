package pers.shawxingkwok.benchmark

import pers.shawxingkwok.kdatastore.KDataStore
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

object KDS: KDataStore("benchmark", cipher) {
    val s1 by store("")
    val s2 by store("")
    val s3 by store("")
    val s4 by store("")
    val s5 by store("")
    val s6 by store("")
    val s7 by store("")
    val s8 by store("")
    val s9 by store("")
    val s10 by store("")
    val s11 by store("")
    val s12 by store("")
    val s13 by store("")
    val s14 by store("")
    val s15 by store("")
    val s16 by store("")
    val s17 by store("")
    val s18 by store("")
    val s19 by store("")
    val s20 by store("")
    val s21 by store("")
    val s22 by store("")
    val s23 by store("")
    val s24 by store("")
    val s25 by store("")
    val s26 by store("")
    val s27 by store("")
    val s28 by store("")
    val s29 by store("")
    val s30 by store("")
}