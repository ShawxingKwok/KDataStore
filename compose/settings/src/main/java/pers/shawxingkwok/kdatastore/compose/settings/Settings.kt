package pers.shawxingkwok.kdatastore.compose.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val isDarkMode by nullableBool()
    val info by nullableKtSerializable<Info>()
}