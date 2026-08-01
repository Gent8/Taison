package mihon.core.migration.migrations

import app.cash.sqldelight.db.SqlDriver
import logcat.LogPriority
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

/**
 * Devices that already had Taison's own migrations 11/12 (excluded_scanlators view, perf
 * indexes) applied before the v0.20.1f sync had those slots renumbered to 14/15, so upstream's
 * real 11-13 (extension_store table, mangas/chapters `memo` columns) got silently skipped by
 * SQLDelight's version counter on upgrade. Repair whatever's still missing, idempotently,
 * regardless of which migration path a given install took to get here.
 */
class RepairExtensionStoreMigration : Migration {
    override val version: Float = 131f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val driver = migrationContext.get<SqlDriver>() ?: return@withIOContext false

        runSql(driver) {
            """
            |CREATE TABLE IF NOT EXISTS extension_store(
            |    index_url TEXT NOT NULL PRIMARY KEY,
            |    name TEXT NOT NULL,
            |    badge_label TEXT NOT NULL,
            |    signing_key TEXT NOT NULL,
            |    contact_website TEXT NOT NULL,
            |    contact_discord TEXT,
            |    is_legacy INTEGER NOT NULL,
            |    extension_list_url TEXT
            |)
            """.trimMargin()
        }

        runSql(driver) {
            """
            |INSERT OR IGNORE INTO extension_store(index_url, name, badge_label, signing_key, contact_website, contact_discord, is_legacy)
            |SELECT base_url || '/repo.json', name, coalesce(short_name, name), signing_key_fingerprint, website, NULL, 1 FROM extension_repos
            """.trimMargin()
        }

        runSql(driver) { "DROP TABLE IF EXISTS extension_repos" }
        runSql(driver) { "ALTER TABLE extension_store ADD COLUMN extension_list_url TEXT" }
        runSql(driver) { "ALTER TABLE mangas ADD COLUMN memo BLOB NOT NULL DEFAULT '{}'" }
        runSql(driver) { "ALTER TABLE chapters ADD COLUMN memo BLOB NOT NULL DEFAULT '{}'" }

        true
    }

    private suspend fun runSql(driver: SqlDriver, sql: () -> String) {
        runCatching {
            driver.execute(null, sql(), 0).await()
        }.onFailure { logcat(LogPriority.INFO, it) { "RepairExtensionStoreMigration: statement skipped" } }
    }
}
