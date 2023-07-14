package pers.apollokwok.kdatastore

public interface Cypher {
    public fun encrypt(bytes: ByteArray): ByteArray
    public fun decrypt(bytes: ByteArray): ByteArray
}