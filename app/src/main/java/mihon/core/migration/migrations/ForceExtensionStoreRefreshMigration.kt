package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Forces the extension stores to refresh on the next launch.
 *
 * Converting `extension_repos` into `extension_store` leaves every store flagged as legacy, which
 * makes the app read the repo's old index. Keiyoushi's now holds nothing but two placeholders
 * telling the user to update, so every installed extension is missing from the available list and
 * gets labelled orphaned — with a banner recommending uninstalling it.
 *
 * A store only stops being legacy once `refreshAll` follows its `index_v2` link, and the one caller
 * that does so on launch is rate limited to a single check per day. An install that happens to have
 * checked recently would sit with everything orphaned until that window elapses, so clear the
 * timestamp and let the refresh run immediately.
 */
class ForceExtensionStoreRefreshMigration : Migration {
    override val version: Float = 131f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false

        val lastExtCheck = preferenceStore.getLong(Preference.appStateKey("last_ext_check"), 0)
        if (lastExtCheck.isSet()) lastExtCheck.delete()

        return@withIOContext true
    }
}
