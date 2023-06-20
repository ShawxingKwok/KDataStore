package pers.shawxingkwok.kdatastore.viewkt.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val theme: Flow<Theme> by storeEnum(Theme.FOLLOW_SYSTEM)
}