package pers.shawxingkwok.kdatastore.examples.view

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore(){
    val isDarkMode by bool(false)
}