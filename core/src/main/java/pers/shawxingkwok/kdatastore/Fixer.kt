package pers.shawxingkwok.kdatastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

@PublishedApi
internal class Fixer {
    private class FixDataMigration(private val neededKeys: MutableSet<Preferences.Key<*>>)
        : DataMigration<Preferences>
    {
        lateinit var needlessKeys: Set<Preferences.Key<*>>

        override suspend fun shouldMigrate(currentData: Preferences): Boolean {
            needlessKeys = currentData.asMap().keys - neededKeys
            neededKeys.clear()
            return needlessKeys.any()
        }

        override suspend fun migrate(currentData: Preferences): Preferences {
            val prefs = currentData.toMutablePreferences()
            needlessKeys.forEach { prefs -= it }
            return prefs
        }

        override suspend fun cleanUp() {}
    }

    @PublishedApi
    internal val keys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    @PublishedApi
    internal val backupKeys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    @PublishedApi
    internal val generalFixDataMigration: DataMigration<Preferences> = FixDataMigration(keys)

    @PublishedApi
    internal val backupFixDataMigration: DataMigration<Preferences> = FixDataMigration(backupKeys)
}