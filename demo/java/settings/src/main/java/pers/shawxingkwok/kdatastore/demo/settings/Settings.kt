package pers.shawxingkwok.kdatastore.demo.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    // `@JvmStatic` is needless if never called from Java.
    @JvmStatic
    val isDarkMode by storeNullable<Boolean>()
}