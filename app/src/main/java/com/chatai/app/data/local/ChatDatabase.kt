package com.chatai.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chatai.app.data.local.dao.CodeAttemptDao
import com.chatai.app.data.local.dao.ConversationDao
import com.chatai.app.data.local.dao.MessageDao
import com.chatai.app.data.local.entity.CodeAttemptEntity
import com.chatai.app.data.local.entity.ConversationEntity
import com.chatai.app.data.local.entity.MessageEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN imageType TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN galleryId TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `code_attempts` (
                `id` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `sourceCode` TEXT NOT NULL,
                `problemDescription` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `passedTests` INTEGER NOT NULL,
                `totalTests` INTEGER NOT NULL,
                `counterexampleInput` TEXT,
                `expectedOutput` TEXT,
                `actualOutput` TEXT,
                `details` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )""".trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_code_attempts_createdAt` " +
                "ON `code_attempts` (`createdAt`)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN attachmentsJson TEXT")
    }
}

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, CodeAttemptEntity::class],
    version = 5,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun codeAttemptDao(): CodeAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
                // If migration still fails, recreate DB rather than crash
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
