package pers.apollokwok.prefstore.example.view.data.ds

import pers.apollokwok.prefstore.EasyDSFlow
import pers.apollokwok.prefstore.PrefStore
import pers.apollokwok.prefstore.Encryption

object Settings : PrefStore(
    encryption = Encryption.AES(
        password = "password",
        salt = "s",
        ivBytes =
            listOf(
                0x16, 0x09, 0xc0, 0x4d,
                0x4a, 0x09, 0xd2, 0x46,
                0x71, 0xcc, 0x32, 0xb7,
                0xd2, 0x91, 0x8a, 0x9c,
            )
            .map { it.toByte() }
            .toByteArray(),
    ),
    defaultEncrypted = false,
){
    val language: EasyDSFlow<Language> by enum(Language.ENGLISH)
    val location by ktSerializable(Location(38.2, 55.3), true)
}