package pers.shawxingkwok.kdatastore.viewjava.settings

import pers.shawxingkwok.kdatastore.KDataStore

// @JvmStatic is needless if not used in java
object Settings : KDataStore("settings") {
    @JvmStatic
    val theme by storeEnum(Theme.FOLLOW_SYSTEM)
}