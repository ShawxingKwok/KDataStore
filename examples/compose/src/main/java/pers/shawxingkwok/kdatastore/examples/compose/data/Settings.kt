package pers.shawxingkwok.kdatastore.examples.compose.data

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore(){
    val isDarkMode by bool(false)
}