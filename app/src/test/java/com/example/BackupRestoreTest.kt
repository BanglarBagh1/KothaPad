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
}
