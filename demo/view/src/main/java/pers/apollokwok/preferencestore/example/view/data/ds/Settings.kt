package pers.apollokwok.preferencestore.example.view.data.ds

import pers.apollokwok.preferencestore.Encryption
import pers.apollokwok.preferencestore.PreferenceStore

object Settings : PreferenceStore(
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
    val language by enum(Language.ENGLISH)
    val location by ktSerializable(Location(38.2, 55.3), true)
    val name by string("Apollo")
}