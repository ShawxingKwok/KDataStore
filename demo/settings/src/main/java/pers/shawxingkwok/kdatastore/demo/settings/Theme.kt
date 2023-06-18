package pers.shawxingkwok.kdatastore.demo.settings

import androidx.appcompat.app.AppCompatDelegate

enum class Theme(val text: String, val value: Int) {
    FOLLOW_SYSTEM("follow system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
}