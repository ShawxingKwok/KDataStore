package pers.shawxingkwok.kdatastore.demo.settings

import pers.shawxingkwok.kdatastore.KDataStore

object Settings : KDataStore("settings") {
    val theme by enum(Theme.FOLLOW_SYSTEM)

    // simulation
    val allRoles by ktSerializable(
        setOf(
            Role("Apollo", 25),
            Role("Bruce", 27),
            Role("James", 30),
        )
    )

    val currentRole by nullableKtSerializable<Role>()
}