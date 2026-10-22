package com.pixilio.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val editor = Editor()
    private var pendingScale = 1
    private val autosaveFile by lazy { File(filesDir, "autosave.pixilio") }

    private fun toast(s: String) {
        runOnUiThread { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
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

        if (autosaveFile.exists()) {
            Thread {
                try {
                    val snap = FileInputStream(autosaveFile).use { ProjectIO.read(it) }
                    runOnUiThread { ProjectIO.apply(editor, snap) }
                } catch (t: Throwable) {
                    // autosave rusak: abaikan
                }
            }.start()
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
                App(editor, actions)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val snap = ProjectIO.snapshot(editor)
            Thread {
                try {
                    val tmp = File(filesDir, "autosave.tmp")
                    FileOutputStream(tmp).use { ProjectIO.write(snap, it) }
                    tmp.renameTo(autosaveFile)
                } catch (t: Throwable) {
                    // abaikan
                }
            }.start()
        } catch (t: Throwable) {
            // abaikan
        }
    }
}
