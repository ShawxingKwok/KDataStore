package pers.apollokwok.preferencestore.example.compose.data.ds

import android.content.Context
import pers.apollokwok.preferencestore.Flow
import pers.apollokwok.preferencestore.PreferenceStore
import pers.apollokwok.preferencestore.Encryption

object Settings : PreferenceStore(
    encryption = Encryption.AES(
        password = "password",
        salt = "salt",
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
    val language: Flow<Language> by enum(Language.ENGLISH)
    val location by ktSerializable(Location(38.2, 55.3), true)

    override fun onWholeCorrupt() {}

    override fun getMigration(context: Context): Migration? = null
}