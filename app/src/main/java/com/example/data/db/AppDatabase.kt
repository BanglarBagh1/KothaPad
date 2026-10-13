package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Note
import com.example.data.model.SpokenCommand
import com.example.data.model.Stamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Note::class, Stamp::class, SpokenCommand::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun stampDao(): StampDao
    abstract fun spokenCommandDao(): SpokenCommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN folder TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE notes ADD COLUMN sessionDurationSec INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stamps ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kothapad.db"
                ).addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            seedInitialData(getInstance(context, scope))
                        }
                    }
                }).build().also { INSTANCE = it }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val stampDao = database.stampDao()
            val noteDao = database.noteDao()
            val commandDao = database.spokenCommandDao()

            // Seed default templates
            if (stampDao.getCount() == 0) {
                val defaultTemplates = listOf(
                    Stamp(
                        setName = "Journal",
                        label = "Daily journal",
                        templateText = """═══ Daily Journal: {date} ({weekday}) ═══
Today:
{cursor}

Gratitude:
- 

Tomorrow:
- """,
                        voiceTriggerPhrase = "daily journal",
                        orderIndex = 0,
                        isPinned = true
                    ),
                    Stamp(
                        setName = "Study",
                        label = "Study log",
                        templateText = """═══ Study Log: {datetime} ═══
Subject: {cursor}
Key Concepts:
- 

Questions & Follow-ups:
- """,
                        voiceTriggerPhrase = "study log",
                        orderIndex = 1,
                        isPinned = false
                    ),
                    Stamp(
                        setName = "General",
                        label = "Quick note",
                        templateText = "[{date} {time}] {cursor}",
                        voiceTriggerPhrase = "quick note",
                        orderIndex = 2,
                        isPinned = false
                    )
                )
                stampDao.insertAll(defaultTemplates)
            }

            // Seed spoken commands
            if (commandDao.getCount() == 0) {
                val defaultCommands = listOf(
                    // English Punctuation
                    SpokenCommand(language = "en", phrase = "full stop", actionType = "PUNCTUATION", replacementText = ". "),
                    SpokenCommand(language = "en", phrase = "period", actionType = "PUNCTUATION", replacementText = ". "),
                    SpokenCommand(language = "en", phrase = "comma", actionType = "PUNCTUATION", replacementText = ", "),
                    SpokenCommand(language = "en", phrase = "question mark", actionType = "PUNCTUATION", replacementText = "? "),
                    SpokenCommand(language = "en", phrase = "exclamation mark", actionType = "PUNCTUATION", replacementText = "! "),
                    SpokenCommand(language = "en", phrase = "new line", actionType = "PUNCTUATION", replacementText = "\n"),
                    SpokenCommand(language = "en", phrase = "new paragraph", actionType = "PUNCTUATION", replacementText = "\n\n"),
                    SpokenCommand(language = "en", phrase = "colon", actionType = "PUNCTUATION", replacementText = ": "),
                    SpokenCommand(language = "en", phrase = "dash", actionType = "PUNCTUATION", replacementText = " — "),

                    // Bangla Punctuation
                    SpokenCommand(language = "bn", phrase = "দাঁড়ি", actionType = "PUNCTUATION", replacementText = "। "),
                    SpokenCommand(language = "bn", phrase = "দাড়ি", actionType = "PUNCTUATION", replacementText = "। "),
                    SpokenCommand(language = "bn", phrase = "কমা", actionType = "PUNCTUATION", replacementText = ", "),
                    SpokenCommand(language = "bn", phrase = "প্রশ্নবোধক চিহ্ন", actionType = "PUNCTUATION", replacementText = "? "),
                    SpokenCommand(language = "bn", phrase = "প্রশ্নচিহ্ন", actionType = "PUNCTUATION", replacementText = "? "),
                    SpokenCommand(language = "bn", phrase = "আশ্চর্যবোধক চিহ্ন", actionType = "PUNCTUATION", replacementText = "! "),
                    SpokenCommand(language = "bn", phrase = "নতুন লাইন", actionType = "PUNCTUATION", replacementText = "\n"),
                    SpokenCommand(language = "bn", phrase = "নতুন প্যারাগ্রাফ", actionType = "PUNCTUATION", replacementText = "\n\n"),
                    SpokenCommand(language = "bn", phrase = "কোলন", actionType = "PUNCTUATION", replacementText = ": "),

                    // Voice Actions
                    SpokenCommand(language = "en", phrase = "delete last word", actionType = "ACTION", commandCode = "DELETE_LAST_WORD"),
                    SpokenCommand(language = "en", phrase = "undo that", actionType = "ACTION", commandCode = "UNDO"),
                    SpokenCommand(language = "en", phrase = "stop dictation", actionType = "ACTION", commandCode = "STOP_DICTATION"),
                    SpokenCommand(language = "bn", phrase = "শেষ শব্দ মুছুন", actionType = "ACTION", commandCode = "DELETE_LAST_WORD"),
                    SpokenCommand(language = "bn", phrase = "শব্দ কাটো", actionType = "ACTION", commandCode = "DELETE_LAST_WORD"),
                    SpokenCommand(language = "bn", phrase = "আনডু করো", actionType = "ACTION", commandCode = "UNDO"),
                    SpokenCommand(language = "bn", phrase = "থামো", actionType = "ACTION", commandCode = "STOP_DICTATION"),
                    SpokenCommand(language = "bn", phrase = "বন্ধ করো", actionType = "ACTION", commandCode = "STOP_DICTATION")
                )
                commandDao.insertAll(defaultCommands)
            }

            // Seed initial "Quick tips" Note
            if (noteDao.getNoteCount() == 0) {
                val quickTipsNote = Note(
                    title = "⚡ KothaPad Quick Tips",
                    content = """স্বাগতম KothaPad-এ! (Welcome to KothaPad!)

KothaPad is engineered for power users who dictate at speed:

1. 🎙️ Continuous Non-Stop Dictation
Tap the mic button at the bottom-right. Speak freely in Bangla or English. Take long pauses—the supervisor keeps running and never loses your words.

2. ✍️ Spoken Punctuation
- Say "দাঁড়ি" or "দাড়ি" to insert Bengali dā̃ṛi (।)
- Say "কমা" to insert comma (,)
- Say "নতুন লাইন" for new line
- Say "full stop", "question mark", "exclamation mark"

3. ⌨️ In-App Custom Keyboard
Tap the keyboard button on the secondary toolbar to switch between the custom in-app keyboard and Android system keyboard. Use cursor keys (◀ ▶) while dictation is actively listening!

4. 🏷️ One-Tap Text Stamps
Tap any stamp above the keyboard (Date, Time, Signature, Greetings) to insert dynamic templates right at your cursor.

5. 🛡️ 100% Private & Local-First
All your notes stay safely on this device. No accounts, no cloud sync, no tracking. Auto-saved as you speak.
""".trimIndent(),
                    isPinned = true,
                    languageCode = "bn-BD",
                    folder = "General",
                    tags = "tips,welcome",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                noteDao.insert(quickTipsNote)
            }
        }
    }
}
