package com.clover.studio.exampleapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clover.studio.exampleapp.data.AppDatabase.Companion.DATABASE_VERSION
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.models.*
import com.clover.studio.exampleapp.utils.helpers.TypeConverter

@Database(
    entities = [User::class, Reaction::class, Message::class, PhoneUser::class, ChatRoom::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun reactionDao(): ReactionDao
    abstract fun userDao(): UserDao
    abstract fun phoneUserDao(): PhoneUserDao
    abstract fun chatRoomDao(): ChatRoomDao

    class TablesInfo {
        companion object {
            // list of tables
            const val TABLE_USER = "user"
            const val TABLE_REACTION = "reaction"
            const val TABLE_MESSAGE = "message"
            const val TABLE_PHONE_USER = "phone_user"
            const val TABLE_CHAT_ROOM = "room"

            // general field names
            const val ID = "id"
        }
    }

    companion object {
        const val DATABASE_VERSION = 1

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
                .build()
    }
}