package pers.shawxingkwok.kdatastore.viewjava.settings

import androidx.appcompat.app.AppCompatDelegate

enum class Theme(val value: Int) {
    FOLLOW_SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
}