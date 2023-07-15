package pers.shawxingkwok.kdatastore

public interface Cipher {
    public fun encrypt(bytes: ByteArray): ByteArray
    public fun decrypt(bytes: ByteArray): ByteArray
}