package pers.shawxingkwok.kdatastore.viewkt.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val theme by enum(Theme.FOLLOW_SYSTEM)
    val info by nullableKtSerializable<Info>()
}