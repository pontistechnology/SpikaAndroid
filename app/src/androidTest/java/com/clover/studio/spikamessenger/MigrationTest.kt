package com.clover.studio.spikamessenger

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clover.studio.spikamessenger.data.AppDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @JvmField
    @Rule
    var helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), Objects.requireNonNull(
            AppDatabase::class.java.canonicalName
        ), FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Implement below migration test when testing actual migrations. Change function and adapt to
     * your needs
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        val currentVersion = 3
        val newVersion = 4
        testMigration(currentVersion, newVersion, AppDatabase.MIGRATION_3_4)
    }

    /** Migration 4 to 5 test, when needed */
    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        val currentVersion = 4
        val newVersion = 5
        testMigration(currentVersion, newVersion, AppDatabase.MIGRATION_4_5)
    }

    @Throws(IOException::class)
    private fun testMigration(currentVersion: Int, newVersion: Int, migration: Migration) {
        var db: SupportSQLiteDatabase = helper.createDatabase(TEST_DB, currentVersion)
        db.execSQL(
            "INSERT INTO " + AppDatabase.TablesInfo.TABLE_CHAT_ROOM + " (created_at, name, type, muted, pinned, unread_count, room_exit, deleted) VALUES ('" + System.currentTimeMillis()
                .toString() + "', 'TestName', 'TestType', '1', '0', '3', '1', '0')"
        )
        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, newVersion, true, migration)
        val result = db.isDatabaseIntegrityOk
        Timber.i(
            "%s Migration from %d to %d, isDatabaseIntegrityOk(): %s",
            TEST_DB,
            currentVersion,
            newVersion,
            result
        )
    }

    companion object {
        private const val TEST_DB = "spika-ultimate-migration-test"
    }
}