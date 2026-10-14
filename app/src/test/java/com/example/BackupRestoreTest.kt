package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.Note
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.NoteRepository
import com.example.data.repository.SpokenCommandRepository
import com.example.data.repository.StampRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var noteRepo: NoteRepository
    private lateinit var stampRepo: StampRepository
    private lateinit var commandRepo: SpokenCommandRepository
    private lateinit var prefManager: PreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val scope = CoroutineScope(Dispatchers.IO)
        db = AppDatabase.getInstance(context, scope)
        noteRepo = NoteRepository(db.noteDao())
        stampRepo = StampRepository(db.stampDao())
        commandRepo = SpokenCommandRepository(db.spokenCommandDao())
        prefManager = PreferencesManager(context)
    }

    @Test
    fun testExportAndValidateRoundTrip() = runBlocking {
        // Insert sample notes with folder hierarchy
        val note1 = Note(
            title = "Test Note 1",
            content = "Content for note 1",
            folder = "Work/Projects",
            tags = "work,urgent",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val note2 = Note(
            title = "Personal Journal",
            content = "My diary entry",
            folder = "Personal",
            tags = "journal",
            createdAt = 3000L,
            updatedAt = 4000L
        )
        noteRepo.insert(note1)
        noteRepo.insert(note2)

        val template1 = Stamp(
            setName = "Custom",
            label = "Meeting Agenda",
            templateText = "1. Goals\n2. Actions",
            orderIndex = 0
        )
        stampRepo.insert(template1)

        prefManager.setDictationMode("TIMED")
        prefManager.setAppTheme("LIGHT")

        // Export to byte stream
        val outStream = ByteArrayOutputStream()
        val summary = BackupManager.exportBackup(
            context = context,
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            outputStream = outStream
        )

        assertTrue(summary.notesCount >= 2)
        assertTrue(summary.templatesCount >= 1)

        // Validate and parse
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val parsed = BackupManager.validateAndParseBackup(inStream)

        assertEquals(BackupManager.CURRENT_BACKUP_VERSION, parsed.backupVersion)
        assertTrue(parsed.notes.any { it.title == "Test Note 1" && it.folder == "Work/Projects" })
        assertTrue(parsed.notes.any { it.title == "Personal Journal" && it.folder == "Personal" })
        assertTrue(parsed.stamps.any { it.label == "Meeting Agenda" })
    }

    @Test
    fun testCorruptZipThrowsClearError() = runBlocking {
        val corruptBytes = "Not a real zip archive at all".toByteArray()
        val inStream = ByteArrayInputStream(corruptBytes)
        try {
            BackupManager.validateAndParseBackup(inStream)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("ZIP") == true || e.message?.contains("empty or corrupted") == true)
        }
    }

    @Test
    fun testUnsupportedVersionThrowsClearError() = runBlocking {
        val outStream = ByteArrayOutputStream()
        ZipOutputStream(outStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(BackupManager.MANIFEST_FILE))
            val manifest = """{"backupVersion": 999, "appVersion": "99.0"}"""
            zipOut.write(manifest.toByteArray())
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.NOTES_FILE))
            zipOut.write("[]".toByteArray())
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.TEMPLATES_FILE))
            zipOut.write("[]".toByteArray())
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.SETTINGS_FILE))
            zipOut.write("{}".toByteArray())
            zipOut.closeEntry()
        }

        val inStream = ByteArrayInputStream(outStream.toByteArray())
        try {
            BackupManager.validateAndParseBackup(inStream)
            fail("Should have thrown IllegalArgumentException for unsupported version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unsupported backup version") == true)
        }
    }

    @Test
    fun testPreferencesTypesExportAndImportRoundTrip() = runBlocking {
        // Test values: Long (small and large), Int, Float, Boolean, String, and StringSet
        val expectedLargeLong = 9876543210123L
        val expectedSmallLong = 42L
        val expectedInt = 55
        val expectedFloat = 18.5f
        val expectedBoolean = false
        val expectedString = "CONTINUOUS_VOICE"
        val expectedStringSet = setOf("bn-BD", "en-US", "fr-FR", "es-ES")

        // 1. Set large Long and all other types
        prefManager.setLastOpenedNoteId(expectedLargeLong)
        prefManager.setTimedMinutes(expectedInt)
        prefManager.setEditorFontSize(expectedFloat)
        prefManager.setKeepScreenOn(expectedBoolean)
        prefManager.setDictationMode(expectedString)
        prefManager.setFavoriteLanguages(expectedStringSet)

        // Export to ZIP
        val outStream1 = ByteArrayOutputStream()
        BackupManager.exportBackup(
            context = context,
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            outputStream = outStream1
        )

        // Clear / change all preferences to different values
        prefManager.setLastOpenedNoteId(1L)
        prefManager.setTimedMinutes(1)
        prefManager.setEditorFontSize(10.0f)
        prefManager.setKeepScreenOn(true)
        prefManager.setDictationMode("OTHER")
        prefManager.setFavoriteLanguages(setOf("default-only"))

        // Re-import and restore
        val parsed1 = BackupManager.validateAndParseBackup(ByteArrayInputStream(outStream1.toByteArray()))
        val summary1 = BackupManager.restoreBackup(
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            backupData = parsed1
        )

        // Verify identical values after restore
        assertEquals(0, summary1.skippedCount)
        assertEquals(expectedLargeLong, prefManager.getLastOpenedNoteIdSync())
        assertEquals(expectedInt, prefManager.timedMinutes.first())
        assertEquals(expectedFloat, prefManager.editorFontSize.first(), 0.001f)
        assertEquals(expectedBoolean, prefManager.keepScreenOn.first())
        assertEquals(expectedString, prefManager.dictationMode.first())
        assertEquals(expectedStringSet, prefManager.favoriteLanguages.first())

        // 2. Also verify small Long specifically (which previously could be deserialized as Integer)
        prefManager.setLastOpenedNoteId(expectedSmallLong)

        val outStream2 = ByteArrayOutputStream()
        BackupManager.exportBackup(
            context = context,
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            outputStream = outStream2
        )

        prefManager.setLastOpenedNoteId(999999L)
        val parsed2 = BackupManager.validateAndParseBackup(ByteArrayInputStream(outStream2.toByteArray()))
        BackupManager.restoreBackup(
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            backupData = parsed2
        )
        assertEquals(expectedSmallLong, prefManager.getLastOpenedNoteIdSync())
    }

    @Test
    fun testLegacyV1BackupRestoreWithIntegerToLongConversion() = runBlocking {
        // Construct an old backup (backupVersion = 1) that has no "type" fields,
        // and where numeric values are raw numbers (e.g. small Long is read as Integer in JSON).
        val outStream = ByteArrayOutputStream()
        ZipOutputStream(outStream).use { zipOut ->
            // Manifest
            zipOut.putNextEntry(ZipEntry(BackupManager.MANIFEST_FILE))
            val manifest = """{"backupVersion": 1, "appVersion": "1.0", "exportTimestamp": 1000}"""
            zipOut.write(manifest.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Notes
            zipOut.putNextEntry(ZipEntry(BackupManager.NOTES_FILE))
            zipOut.write("[]".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Templates
            zipOut.putNextEntry(ZipEntry(BackupManager.TEMPLATES_FILE))
            zipOut.write("[]".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Settings without "type" field (legacy v1 format)
            zipOut.putNextEntry(ZipEntry(BackupManager.SETTINGS_FILE))
            val settingsJson = """
            {
                "preferences": [
                    {"key": "last_opened_note_id", "value": 7},
                    {"key": "timed_minutes", "value": 25},
                    {"key": "editor_font_size", "value": 16.5},
                    {"key": "keep_screen_on", "value": true},
                    {"key": "dictation_mode", "value": "CONTINUOUS"},
                    {"key": "favorite_languages", "value": ["en-US", "bn-BD"]}
                ],
                "syncPrefs": {
                    "last_opened_note_id": 7,
                    "onboarding_completed": true
                }
            }
            """.trimIndent()
            zipOut.write(settingsJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        val parsed = BackupManager.validateAndParseBackup(ByteArrayInputStream(outStream.toByteArray()))
        assertEquals(1, parsed.backupVersion)

        // Restore legacy backup: must not fail with "java.lang.Integer cannot be cast to java.lang.Long"
        val summary = BackupManager.restoreBackup(
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            backupData = parsed
        )

        assertEquals(0, summary.skippedCount)
        // Verify small Long was safely converted and is readable as Long from SharedPreferences and DataStore
        assertEquals(7L, prefManager.getLastOpenedNoteIdSync())
        assertEquals(25, prefManager.timedMinutes.first())
        assertEquals(16.5f, prefManager.editorFontSize.first(), 0.001f)
        assertEquals(true, prefManager.keepScreenOn.first())
        assertEquals("CONTINUOUS", prefManager.dictationMode.first())
        assertEquals(setOf("en-US", "bn-BD"), prefManager.favoriteLanguages.first())
    }

    @Test
    fun testSkippedSettingOnConversionFailure() = runBlocking {
        // If a single setting fails to convert, skip that one setting and continue
        val outStream = ByteArrayOutputStream()
        ZipOutputStream(outStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(BackupManager.MANIFEST_FILE))
            zipOut.write("""{"backupVersion": 2, "appVersion": "1.0"}""".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.NOTES_FILE))
            zipOut.write("[]".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.TEMPLATES_FILE))
            zipOut.write("[]".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(BackupManager.SETTINGS_FILE))
            val settingsJson = """
            {
                "preferences": [
                    {"key": "timed_minutes", "type": "int", "value": "invalid_number_here"},
                    {"key": "dictation_mode", "type": "string", "value": "TEST_MODE"}
                ],
                "syncPrefs": []
            }
            """.trimIndent()
            zipOut.write(settingsJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        val parsed = BackupManager.validateAndParseBackup(ByteArrayInputStream(outStream.toByteArray()))
        val summary = BackupManager.restoreBackup(
            noteRepository = noteRepo,
            stampRepository = stampRepo,
            spokenCommandRepository = commandRepo,
            preferencesManager = prefManager,
            backupData = parsed
        )

        // Invalid setting was skipped, valid setting was restored
        assertEquals(1, summary.skippedCount)
        assertEquals("TEST_MODE", prefManager.dictationMode.first())
    }
}
