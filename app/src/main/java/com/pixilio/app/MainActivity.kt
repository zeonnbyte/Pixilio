package com.pixilio.app

import android.os.Bundle
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
    private var pendingScale = 1
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

    private val pngLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            try {
                val w = editor.width
                val h = editor.height
                val sc = pendingScale
                if (w.toLong() * sc * h * sc > Int.MAX_VALUE.toLong()) {
                    toast("Skala terlalu besar")
                } else {
                    val px = ProjectIO.compositePixels(editor)
                    bg("PNG tersimpan") {
                        val out = contentResolver.openOutputStream(uri) ?: throw IOException("Tidak bisa menulis file")
                        out.use { ProjectIO.writePng(px, w, h, sc, it) }
                    }
                }
            } catch (t: Throwable) {
                fail(t)
            }
        }
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
        Bg.load(prefs.getInt("bg", 32))
        Bg.onChange = { prefs.edit().putInt("bg", Bg.size).apply() }
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
            exportPng = { sc ->
                pendingScale = sc
                pngLauncher.launch("pixilio.png")
            },
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
    }

    override fun onStop() {
        super.onStop()
        val f = current ?: return
        saveTo(f, quiet = true, copy = false)
    }
}
