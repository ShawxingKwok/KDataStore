package pers.shawxingkwok.kdatastore.demo.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val isDarkMode by storeNullable<Boolean>()
}