package pers.shawxingkwok.kdatastore.demo.settings

import pers.shawxingkwok.kdatastore.KDSFlow
import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val isDarkMode: KDSFlow<Boolean?> by storeNullable()
}