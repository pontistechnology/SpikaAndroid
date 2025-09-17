package com.clover.studio.spikamessenger.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.data.AppDatabase.Companion.DATABASE_VERSION
import com.clover.studio.spikamessenger.data.daos.ChatRoomDao
import com.clover.studio.spikamessenger.data.daos.MessageDao
import com.clover.studio.spikamessenger.data.daos.MessageRecordsDao
import com.clover.studio.spikamessenger.data.daos.NotesDao
import com.clover.studio.spikamessenger.data.daos.PhoneUserDao
import com.clover.studio.spikamessenger.data.daos.ReactionDao
import com.clover.studio.spikamessenger.data.daos.RoomUserDao
import com.clover.studio.spikamessenger.data.daos.UserDao
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.entity.Note
import com.clover.studio.spikamessenger.data.models.entity.PhoneUser
import com.clover.studio.spikamessenger.data.models.entity.Reaction
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomUser
import com.clover.studio.spikamessenger.utils.helpers.TypeConverter
import org.json.JSONException
import org.json.JSONObject


@Database(
    entities = [User::class, Reaction::class, Message::class, PhoneUser::class, ChatRoom::class, MessageRecords::class, RoomUser::class, Note::class],
    version = DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun reactionDao(): ReactionDao
    abstract fun userDao(): UserDao
    abstract fun phoneUserDao(): PhoneUserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageRecordsDao(): MessageRecordsDao
    abstract fun notesDao(): NotesDao
    abstract fun roomUserDao(): RoomUserDao

    class TablesInfo {
        companion object {
            // List of tables
            const val TABLE_USER = "user"
            const val TABLE_REACTION = "reaction"
            const val TABLE_MESSAGE = "message"
            const val TABLE_PHONE_USER = "phone_user"
            const val TABLE_CHAT_ROOM = "room"
            const val TABLE_MESSAGE_RECORDS = "message_records"
            const val TABLE_ROOM_USER = "room_user"
            const val TABLE_NOTES = "notes"

            // Tables for removing columns or modifications SQLite cannot handle without dropping
            const val TABLE_CHAT_ROOM_NEW = "room_new"

            // General field names
            const val ID = "id"
        }
    }

    companion object {
        const val DATABASE_VERSION = 9

        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {
                    instance = it
                }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "MainDatabase")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION7_8,
                    MIGRATION_8_9
                )
                .fallbackToDestructiveMigration()
                .build()

        /**
         * Use method below to clear all database tables and get a clean slate
         */
        fun nukeDb() {
            buildDatabase(MainApplication.appContext).clearAllTables()
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE_RECORDS + " ADD COLUMN record_message TEXT")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_USER + " ADD COLUMN is_bot INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_CHAT_ROOM + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TablesInfo.TABLE_CHAT_ROOM_NEW + " (`room_id` INTEGER NOT NULL, `name` TEXT, `type` TEXT, `avatar_file_id` INTEGER, `created_at` INTEGER, `modified_at` INTEGER, `muted` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `room_exit` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `unread_count` INTEGER NOT NULL, PRIMARY KEY(`room_id`))")
                db.execSQL("INSERT INTO " + TablesInfo.TABLE_CHAT_ROOM_NEW + " (room_id, name, type, avatar_file_id, created_at, modified_at, muted, pinned, room_exit, deleted, unread_count) SELECT room_id, name, type, avatar_file_id, created_at, modified_at, muted, pinned, IFNULL(room_exit, 0), deleted, unread_count FROM room")
                db.execSQL("DROP TABLE " + TablesInfo.TABLE_CHAT_ROOM)
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_CHAT_ROOM_NEW + " RENAME TO " + TablesInfo.TABLE_CHAT_ROOM)

                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " RENAME COLUMN type TO type_message")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " RENAME COLUMN room_id TO room_id_message")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " RENAME COLUMN created_at TO created_at_message")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " RENAME COLUMN modified_at TO modified_at_message")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " RENAME COLUMN deleted TO deleted_message")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_USER + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " ADD COLUMN message_status TEXT")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " ADD COLUMN uri TEXT")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_USER + " RENAME COLUMN id TO user_id")
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_ROOM_USER + " RENAME COLUMN id TO user_id")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE_RECORDS + " ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " ADD COLUMN is_forwarded INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " ADD COLUMN reference_message TEXT")

                val cursor = db.query("SELECT * FROM " + TablesInfo.TABLE_MESSAGE)
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val jsonString = cursor.getString(cursor.getColumnIndex("body"))
                    try {
                        val jsonObject = JSONObject(jsonString)
                        val referenceMessage =
                            jsonObject.getJSONObject("referenceMessage").toString()

                        val values = ContentValues()
                        values.put("reference_message", referenceMessage)
                        db.update(
                            TablesInfo.TABLE_MESSAGE,
                            SQLiteDatabase.CONFLICT_REPLACE,
                            values,
                            "body = ?",
                            arrayOf(jsonString)
                        )

                        jsonObject.remove("referenceMessage")
                        val updatedJsonString = jsonObject.toString()

                        val updatedValues = ContentValues()
                        updatedValues.put("body", updatedJsonString)
                        db.update(
                            TablesInfo.TABLE_MESSAGE,
                            SQLiteDatabase.CONFLICT_REPLACE,
                            updatedValues,
                            "body = ?",
                            arrayOf(jsonString)
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    cursor.moveToNext()
                }
                cursor.close()
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE " + TablesInfo.TABLE_MESSAGE + " ADD COLUMN thumb_uri TEXT")
            }
        }
    }
}
