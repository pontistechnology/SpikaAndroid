package com.clover.studio.exampleapp

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clover.studio.exampleapp.data.AppDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class MigrationTest {
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
//    @Test
//    @Throws(IOException::class)
//    fun migrate1To2() {
//        val currentVersion = 1
//        val newVersion = 2
//        testMigration(currentVersion, newVersion, AppDatabase.MIGRATION_1_2)
//    }

    @Throws(IOException::class)
    private fun testMigration(currentVersion: Int, newVersion: Int, migration: Migration) {
        var db: SupportSQLiteDatabase = helper.createDatabase(TEST_DB, currentVersion)
        db.execSQL(
            "INSERT INTO " + AppDatabase.TablesInfo.TABLE_CHAT_ROOM + " (id, description, email, name) VALUES ('" + System.currentTimeMillis()
                .toString() + "', 'TestDescription', 'TestEmail', 'TestName')"
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