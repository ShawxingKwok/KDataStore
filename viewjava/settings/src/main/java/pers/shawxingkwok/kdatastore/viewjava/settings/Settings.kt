package pers.shawxingkwok.kdatastore.viewjava.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    // '@JvmStatic' is needless if not used in java
    @JvmStatic
    val theme by storeEnum(Theme.FOLLOW_SYSTEM)
}