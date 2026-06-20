package com.presto.mediamanager.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.presto.mediamanager.data.db.AppDatabase
import com.presto.mediamanager.data.db.MediaDao
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.fakes.FakeSettingsProvider
import com.presto.mediamanager.fakes.FakeStorageGateway
import com.presto.mediamanager.fakes.FakeVideoExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Exercises the real repository + real (in-memory) Room against a temp-directory
 * storage fake — the full scan / delete / archive / auto-delete flow on the JVM,
 * no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.presto.mediamanager.TestApp::class)
class MediaRepositoryFlowTest {

    private val day = 24L * 60L * 60L * 1000L
    private lateinit var db: AppDatabase
    private lateinit var dao: MediaDao
    private lateinit var storage: FakeStorageGateway
    private lateinit var settings: FakeSettingsProvider
    private lateinit var repo: MediaRepository

    private lateinit var inputDir: File
    private lateinit var archiveDir: File
    private lateinit var shareDir: File
    private var clock = 1_700_000_000_000L

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.mediaDao()

        inputDir = java.nio.file.Files.createTempDirectory("input").toFile()
        archiveDir = java.nio.file.Files.createTempDirectory("archive").toFile()
        shareDir = java.nio.file.Files.createTempDirectory("share").toFile()
        storage = FakeStorageGateway()
        settings = FakeSettingsProvider(
            AppSettings(
                inputFolderUri = inputDir.absolutePath,
                archiveFolderUri = archiveDir.absolutePath,
                shareFolderUri = shareDir.absolutePath,
            ),
        )
        repo = MediaRepository(dao, storage, settings, FakeVideoExporter(), now = { clock })
    }

    @After
    fun tearDown() {
        db.close()
        listOf(inputDir, archiveDir, shareDir).forEach { it.deleteRecursively() }
    }

    private fun writeVideo(dir: File, name: String, capturedMs: Long = clock): File =
        File(dir, name).apply {
            writeBytes(ByteArray(16) { 1 })
            setLastModified(capturedMs)
        }

    @Test
    fun scan_addsNewVideos() = runTest {
        writeVideo(inputDir, "a.mp4")
        writeVideo(inputDir, "b.mp4")

        assertThat(repo.scanInputFolder()).isEqualTo(2)
        assertThat(repo.observeReviewQueue().first()).hasSize(2)

        // Re-scan is idempotent.
        assertThat(repo.scanInputFolder()).isEqualTo(0)
    }

    @Test
    fun delete_removesFileAndRow() = runTest {
        val f = writeVideo(inputDir, "a.mp4")
        repo.scanInputFolder()
        val item = repo.observeReviewQueue().first().single()

        repo.delete(item)

        assertThat(f.exists()).isFalse()
        assertThat(repo.reviewCount()).isEqualTo(0)
    }

    @Test
    fun quickArchive_copiesWithDatedNameAndRemovesSource() = runTest {
        val captured = 1_718_000_000_000L
        val src = writeVideo(inputDir, "a.mp4", capturedMs = captured)
        repo.scanInputFolder()
        val item = repo.observeReviewQueue().first().single()

        repo.quickArchive(item, "near miss")

        assertThat(src.exists()).isFalse()
        val archived = archiveDir.listFiles()!!.map { it.name }
        assertThat(archived).hasSize(1)
        assertThat(archived.single()).matches("\\d{4}-\\d{2}-\\d{2}_near-miss\\.mp4")
        assertThat(repo.reviewCount()).isEqualTo(0)
    }

    @Test
    fun scan_dropsVanishedRows() = runTest {
        val a = writeVideo(inputDir, "a.mp4")
        writeVideo(inputDir, "b.mp4")
        repo.scanInputFolder()
        assertThat(repo.reviewCount()).isEqualTo(2)

        a.delete()
        repo.scanInputFolder()

        assertThat(repo.reviewCount()).isEqualTo(1)
    }

    @Test
    fun autoDelete_removesOnlyStale() = runTest {
        writeVideo(inputDir, "old.mp4")
        repo.scanInputFolder() // old: firstSeen = clock

        clock += 20 * day
        writeVideo(inputDir, "new.mp4")
        repo.scanInputFolder() // new: firstSeen = clock + 20d

        val removed = repo.autoDeleteOlderThan(14)

        assertThat(removed).isEqualTo(1)
        val remaining = repo.observeReviewQueue().first()
        assertThat(remaining.single().displayName).isEqualTo("new.mp4")
    }

    @Test
    fun later_keepsItemInQueue() = runTest {
        writeVideo(inputDir, "a.mp4")
        repo.scanInputFolder()
        val item = repo.observeReviewQueue().first().single()

        repo.markLater(item)

        // LATER still counts toward the review queue (and auto-delete).
        assertThat(repo.reviewCount()).isEqualTo(1)
    }
}
