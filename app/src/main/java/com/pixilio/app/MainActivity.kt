package com.pixilio.app

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val editor = Editor()
    private var pendingExport: ExportJob? = null
    private val autosaveFile by lazy { File(filesDir, "autosave.pixilio") }
    private val rootDir by lazy { ProjectStore.root(filesDir) }
    private var dir: File by mutableStateOf(File(""))
    private var current by mutableStateOf<File?>(null)
    private var refresh by mutableIntStateOf(0)

    private fun saveTo(f: File, quiet: Boolean = false, copy: Boolean = true, done: (() -> Unit)? = null) {
        val snap = try {
            ProjectIO.snapshot(editor, copy)
        } catch (t: Throwable) {
            if (!quiet) fail(t)
            return
        }
        Thread {
            try {
                val tmp = File(f.parentFile, f.name + ".tmp")
                FileOutputStream(tmp).use { ProjectIO.write(snap, it) }
                if (!tmp.renameTo(f)) {
                    f.delete()
                    tmp.renameTo(f)
                }
                try {
                    ProjectIO.writeThumb(snap, ProjectStore.thumb(f))
                } catch (_: Throwable) {
                }
            } catch (t: Throwable) {
                if (!quiet) fail(t)
            }
            if (done != null) runOnUiThread(done)
        }.start()
    }

    private fun goHome() {
        val f = current ?: return
        editor.commitFloat()
        saveTo(f, copy = false) {
            current = null
            refresh++
        }
    }

    private fun openEntry(f: File) {
        Thread {
            try {
                val snap = FileInputStream(f).use { ProjectIO.read(it) }
                runOnUiThread {
                    ProjectIO.apply(editor, snap)
                    current = f
                }
            } catch (t: Throwable) {
                fail(t)
            }
        }.start()
    }

    private fun createProject(name: String, w: Int, h: Int) {
        if (!editor.newDoc(w, h)) return
        val f = ProjectStore.projectFile(dir, name)
        current = f
        saveTo(f, copy = false)
    }

    private fun makeFolder(name: String): Boolean {
        val ok = ProjectStore.createFolder(dir, name)
        if (!ok) toast("Nama folder kosong atau sudah dipakai")
        return ok
    }

    private fun renameEntry(e: Entry, name: String): Boolean {
        val ok = ProjectStore.rename(e, name)
        if (!ok) toast("Nama kosong atau sudah dipakai")
        return ok
    }

    private fun toast(s: String) {
        runOnUiThread { Toast.makeText(this, tr(s), Toast.LENGTH_SHORT).show() }
    }

    private fun fail(t: Throwable) {
        toast("Gagal: ${t.message ?: t.javaClass.simpleName}")
    }

    private fun bg(ok: String, block: () -> Unit) {
        Thread {
            try {
                block()
                toast(ok)
            } catch (t: Throwable) {
                fail(t)
            }
        }.start()
    }

    private val saveLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            try {
                val snap = ProjectIO.snapshot(editor)
                bg("Proyek disimpan") {
                    val out = contentResolver.openOutputStream(uri) ?: throw IOException("Tidak bisa menulis file")
                    out.use { ProjectIO.write(snap, it) }
                }
            } catch (t: Throwable) {
                fail(t)
            }
        }
    }

    private val openLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Thread {
                try {
                    val input = contentResolver.openInputStream(uri) ?: throw IOException("Tidak bisa membaca file")
                    val snap = input.use { ProjectIO.read(it) }
                    runOnUiThread {
                        try {
                            ProjectIO.apply(editor, snap)
                            toast("Proyek dibuka")
                        } catch (t: Throwable) {
                            fail(t)
                        }
                    }
                } catch (t: Throwable) {
                    fail(t)
                }
            }.start()
        }
    }

    private val imageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Thread {
                try {
                    val input = contentResolver.openInputStream(uri) ?: throw IOException("Tidak bisa membaca file")
                    val img = input.use { ProjectIO.decodeImage(it) } ?: throw IOException("Bukan gambar yang valid")
                    runOnUiThread {
                        try {
                            if (editor.newDoc(img.w, img.h, img.px, "Gambar")) toast("Gambar diimpor")
                        } catch (t: Throwable) {
                            fail(t)
                        }
                    }
                } catch (t: Throwable) {
                    fail(t)
                }
            }.start()
        }
    }

    private class ExportJob(val px: IntArray, val w: Int, val h: Int, val sc: Int)

    private val storagePerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val job = pendingExport
        pendingExport = null
        if (ok && job != null) doExport(job) else toast("Izin penyimpanan ditolak, ekspor dibatalkan")
    }

    private class ExportName(val name: String, val day: String, val n: Int)

    /** Nama berkas ekspor: "dd.MM.yyyy HH.mm N.png", N = urutan ekspor di hari itu. Baru disimpan setelah ekspor berhasil. */
    private fun exportName(after: Int = 0): ExportName {
        val now = Date()
        val day = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(now)
        val time = SimpleDateFormat("HH.mm", Locale.ROOT).format(now)
        val prefs = getSharedPreferences("pixilio", MODE_PRIVATE)
        val base = if (prefs.getString("exp_day", "") == day) prefs.getInt("exp_n", 0) else 0
        val n = maxOf(base, after) + 1
        return ExportName("$day $time $n.png", day, n)
    }

    private fun commitExport(en: ExportName) {
        getSharedPreferences("pixilio", MODE_PRIVATE).edit()
            .putString("exp_day", en.day).putInt("exp_n", en.n).apply()
    }

    private fun startExport(sc: Int) {
        try {
            val w = editor.width
            val h = editor.height
            if (sc < 1 || w.toLong() * sc * h * sc > Int.MAX_VALUE.toLong()) {
                toast("Skala terlalu besar")
                return
            }
            val job = ExportJob(ProjectIO.compositePixels(editor), w, h, sc)
            if (Build.VERSION.SDK_INT < 29 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingExport = job
                storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                doExport(job)
            }
        } catch (t: Throwable) {
            fail(t)
        }
    }

    private fun doExport(job: ExportJob) {
        Thread {
            try {
                var en = exportName()
                if (Build.VERSION.SDK_INT >= 29) {
                    val cv = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, en.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Pixilio")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                        ?: throw IOException("Tidak bisa membuat file")
                    try {
                        val out = contentResolver.openOutputStream(uri) ?: throw IOException("Tidak bisa menulis file")
                        out.use { ProjectIO.writePng(job.px, job.w, job.h, job.sc, it) }
                        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                        contentResolver.update(uri, done, null, null)
                    } catch (t: Throwable) {
                        contentResolver.delete(uri, null, null)
                        throw t
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Pixilio")
                    if (!dir.exists() && !dir.mkdirs()) throw IOException("Tidak bisa membuat folder Pictures/Pixilio")
                    while (File(dir, en.name).exists()) en = exportName(en.n)
                    val f = File(dir, en.name)
                    FileOutputStream(f).use { ProjectIO.writePng(job.px, job.w, job.h, job.sc, it) }
                    MediaScannerConnection.scanFile(this, arrayOf(f.path), arrayOf("image/png"), null)
                }
                commitExport(en)
                toast("Tersimpan di Pictures/Pixilio/${en.name}")
            } catch (t: Throwable) {
                fail(t)
            }
        }.start()
    }

    private fun displayName(uri: Uri): String {
        var n: String? = null
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) n = it.getString(0)
            }
        } catch (_: Throwable) {
        }
        val raw = n ?: uri.lastPathSegment ?: ""
        val base = raw.substringAfterLast('/').substringBeforeLast('.')
        return base.ifBlank { "Gambar" }
    }

    /** Buka gambar / file .pixilio dari app lain (Buka dengan / Bagikan). */
    private fun handleIntent(i: Intent?) {
        if (i == null) return
        val act = i.action
        if (act != Intent.ACTION_VIEW && act != Intent.ACTION_SEND) return
        val uri: Uri? = if (act == Intent.ACTION_SEND) {
            if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
        } else i.data
        i.action = null
        i.data = null
        if (uri == null) return
        val base = displayName(uri)
        Thread {
            try {
                val raw = contentResolver.openInputStream(uri) ?: throw IOException("Tidak bisa membaca file")
                val bin = BufferedInputStream(raw, 1 shl 16)
                bin.use {
                    it.mark(4)
                    val b0 = it.read()
                    val b1 = it.read()
                    it.reset()
                    if (b0 == 0x1f && b1 == 0x8b) {
                        val snap = ProjectIO.read(it)
                        runOnUiThread {
                            try {
                                val c = current
                                if (c != null) {
                                    editor.commitFloat()
                                    saveTo(c, quiet = true)
                                }
                                val f = ProjectStore.projectFile(rootDir, base)
                                ProjectIO.apply(editor, snap)
                                dir = rootDir
                                current = f
                                saveTo(f, copy = false)
                            } catch (t: Throwable) {
                                fail(t)
                            }
                        }
                    } else {
                        val img = ProjectIO.decodeImage(it) ?: throw IOException("Bukan gambar yang valid")
                        runOnUiThread {
                            try {
                                val c = current
                                if (c != null) {
                                    editor.commitFloat()
                                    saveTo(c, quiet = true)
                                }
                                if (editor.newDoc(img.w, img.h, img.px, "Gambar")) {
                                    val f = ProjectStore.projectFile(rootDir, base)
                                    dir = rootDir
                                    current = f
                                    saveTo(f, copy = false)
                                }
                            } catch (t: Throwable) {
                                fail(t)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                fail(t)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("pixilio", MODE_PRIVATE)
        Lang.load(prefs.getString("lang", "system") ?: "system")
        Lang.onChange = { prefs.edit().putString("lang", Lang.mode).apply() }
        val saved = prefs.getString("palette", null)
        if (!saved.isNullOrEmpty()) {
            val list = saved.split(",").mapNotNull { it.toIntOrNull() }
            if (list.isNotEmpty()) {
                editor.palette.clear()
                editor.palette.addAll(list)
            }
        }
        editor.onPaletteChange = {
            prefs.edit().putString("palette", editor.palette.joinToString(",")).apply()
        }
        editor.onMessage = { toast(it) }
        editor.newDoc(32, 32)

        dir = rootDir
        if (autosaveFile.exists()) {
            // migrasi autosave versi lama menjadi satu proyek
            autosaveFile.renameTo(ProjectStore.projectFile(rootDir, "Proyek lama"))
        }

        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (t: Throwable) {
            ""
        }
        val actions = Actions(
            openProject = { openLauncher.launch(arrayOf("*/*")) },
            saveProject = { saveLauncher.launch("Proyek Pixilio.pixilio") },
            exportPng = { sc -> startExport(sc) },
            importImage = { imageLauncher.launch(arrayOf("image/*")) },
            version = version
        )

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val c = current
                BackHandler(enabled = c != null || dir != rootDir) {
                    if (current != null) goHome() else dir.parentFile?.let { dir = it }
                }
                if (c == null) {
                    HomeScreen(
                        root = rootDir,
                        dir = dir,
                        refresh = refresh,
                        onDir = { dir = it },
                        onOpen = { openEntry(it) },
                        onCreate = { n, w, h -> createProject(n, w, h) },
                        onFolder = { makeFolder(it) },
                        onRename = { e, n -> renameEntry(e, n) },
                        onDelete = { ProjectStore.delete(it) }
                    )
                } else {
                    App(editor, actions, c.name.removeSuffix(ProjectStore.EXT)) { goHome() }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        val f = current ?: return
        saveTo(f, quiet = true, copy = false)
    }
}
