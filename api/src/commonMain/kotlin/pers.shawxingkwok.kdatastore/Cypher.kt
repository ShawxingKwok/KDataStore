package pers.shawxingkwok.kdatastore

public interface Cypher {
    public fun encrypt(bytes: ByteArray): ByteArray
    public fun decrypt(bytes: ByteArray): ByteArray
}