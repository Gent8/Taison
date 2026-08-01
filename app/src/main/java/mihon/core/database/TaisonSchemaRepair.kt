package mihon.core.database

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Repairs installs whose schema version counter no longer matches their actual schema.
 *
 * Taison v1.2.x shipped its own migrations in slots 11 and 12 (the `updatesView` rewrite and the
 * performance indexes). The v0.20.1f sync replaced those slots with upstream's 11-13, which create
 * the `extension_store` table and the `memo` columns. SQLDelight only tracks a version counter, so
 * those installs report a version that makes it skip straight to 13.sqm, which then fails with
 * `no such table: extension_store` and takes the app down before it can draw anything.
 *
 * This has to run before SQLDelight opens the database: once the driver is handed the file it
 * migrates on first use, and by then it's too late to intervene. Detection keys off the actual
 * schema rather than the version alone, so installs that took the upstream path (and therefore
 * already have `extension_store`) are left untouched.
 */
object TaisonSchemaRepair {

    /** Versions reachable only by an install that applied Taison's own 11/12 migrations. */
    private val AFFECTED_VERSIONS = 12L..13L

    /** Schema version once upstream's 11-13 have been accounted for. */
    private const val REPAIRED_VERSION = 14L

    fun repairIfNeeded(context: Context, databaseName: String) {
        val databaseFile = context.getDatabasePath(databaseName)
        // A fresh install has no file yet, and opening one here would create an empty database that
        // SQLDelight would then refuse to migrate.
        if (!databaseFile.exists()) return

        try {
            BundledSQLiteDriver().open(databaseFile.absolutePath).use { connection ->
                val version = connection.userVersion()
                if (version !in AFFECTED_VERSIONS) return
                // Present means the install migrated through upstream's 11.sqm and is consistent.
                if (connection.hasTable("extension_store")) return

                logcat(LogPriority.INFO) {
                    "Repairing schema skipped by the v1.3.0 migration renumbering (version $version)"
                }
                connection.repair()
                logcat(LogPriority.INFO) { "Schema repaired, version is now $REPAIRED_VERSION" }
            }
        } catch (e: Throwable) {
            // Leave the database untouched and let SQLDelight surface the real failure.
            logcat(LogPriority.ERROR, e) { "Failed to repair schema" }
        }
    }

    private fun SQLiteConnection.repair() {
        execSQL("BEGIN IMMEDIATE")
        try {
            // 11.sqm
            execSQL(
                """
                |CREATE TABLE extension_store(
                |    index_url TEXT NOT NULL PRIMARY KEY,
                |    name TEXT NOT NULL,
                |    badge_label TEXT NOT NULL,
                |    signing_key TEXT NOT NULL,
                |    contact_website TEXT NOT NULL,
                |    contact_discord TEXT,
                |    is_legacy INTEGER NOT NULL,
                |    extension_list_url TEXT
                |)
                """.trimMargin(),
            )
            if (hasTable("extension_repos")) {
                execSQL(
                    """
                    |INSERT INTO extension_store(
                    |    index_url, name, badge_label, signing_key, contact_website, contact_discord, is_legacy
                    |)
                    |SELECT base_url || '/repo.json', name, coalesce(short_name, name), signing_key_fingerprint,
                    |    website, NULL, 1
                    |FROM extension_repos
                    """.trimMargin(),
                )
                execSQL("DROP TABLE extension_repos")
            }

            // 12.sqm — already applied when the install is at version 13.
            if (!hasColumn("mangas", "memo")) {
                execSQL("ALTER TABLE mangas ADD COLUMN memo BLOB NOT NULL DEFAULT '{}'")
            }
            if (!hasColumn("chapters", "memo")) {
                execSQL("ALTER TABLE chapters ADD COLUMN memo BLOB NOT NULL DEFAULT '{}'")
            }

            // 13.sqm is folded into the CREATE TABLE above.
            execSQL("PRAGMA user_version = $REPAIRED_VERSION")
            execSQL("COMMIT")
        } catch (e: Throwable) {
            execSQL("ROLLBACK")
            throw e
        }
    }

    private fun SQLiteConnection.userVersion(): Long =
        prepare("PRAGMA user_version").use { if (it.step()) it.getLong(0) else -1L }

    private fun SQLiteConnection.hasTable(name: String): Boolean =
        prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use {
            it.bindText(1, name)
            it.step()
        }

    private fun SQLiteConnection.hasColumn(table: String, column: String): Boolean =
        prepare("SELECT 1 FROM pragma_table_info(?) WHERE name = ?").use {
            it.bindText(1, table)
            it.bindText(2, column)
            it.step()
        }
}
