package pers.shawxingkwok.kdatastore

import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.json.Json

public interface Cipher {
    public fun encrypt(bytes: ByteArray): ByteArray
    public fun decrypt(bytes: ByteArray): ByteArray
}

internal fun String.encryptIfNeeded(cipher: Cipher?) =
    if (cipher != null) {
        val utfBytes = encodeToByteArray()
        val base64Bytes = cipher.encrypt(utfBytes)
        Json.encodeToString(ByteArraySerializer(), base64Bytes)
    } else
        this

internal fun String.decryptIfNeeded(cipher: Cipher?) =
    if (cipher != null) {
        val base64Bytes = Json.decodeFromString(ByteArraySerializer(), this)
        val utf8Bytes = cipher.decrypt(base64Bytes)
        String(utf8Bytes)
    } else
        this