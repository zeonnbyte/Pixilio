package com.pixilio.app

import java.io.File

class Entry(val file: File, val isDir: Boolean) {
    val name: String get() = if (isDir) file.name else file.name.removeSuffix(ProjectStore.EXT)
}

object ProjectStore {
    const val EXT = ".pixilio"

    fun thumb(f: File): File = File(f.parentFile, f.name + ".png")

    fun root(filesDir: File): File = File(filesDir, "projects").also { it.mkdirs() }

    fun clean(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ").trim().trim('.').trim().take(60)

    fun list(dir: File): List<Entry> {
        val fs = dir.listFiles() ?: return emptyList()
        val ds = fs.filter { it.isDirectory }.sortedBy { it.name.lowercase() }.map { Entry(it, true) }
        val ps = fs.filter { it.isFile && it.name.endsWith(EXT) }
            .sortedByDescending { it.lastModified() }.map { Entry(it, false) }
        return ds + ps
    }

    fun count(dir: File): Int = dir.listFiles()?.count { it.isDirectory || it.name.endsWith(EXT) } ?: 0

    fun projectFile(dir: File, name: String): File {
        val base = clean(name).ifEmpty { "Proyek baru" }
        var f = File(dir, base + EXT)
        var i = 2
        while (f.exists()) {
            f = File(dir, "$base $i$EXT")
            i++
        }
        return f
    }

    fun createFolder(dir: File, name: String): Boolean {
        val n = clean(name)
        if (n.isEmpty()) return false
        val f = File(dir, n)
        if (f.exists()) return false
        return f.mkdirs()
    }

    fun rename(e: Entry, name: String): Boolean {
        val n = clean(name)
        if (n.isEmpty()) return false
        val t = File(e.file.parentFile, if (e.isDir) n else n + EXT)
        if (t.exists()) return false
        val ok = e.file.renameTo(t)
        if (ok && !e.isDir) {
            val old = thumb(e.file)
            if (old.exists()) old.renameTo(thumb(t))
        }
        return ok
    }

    fun delete(e: Entry) {
        e.file.deleteRecursively()
        if (!e.isDir) thumb(e.file).delete()
    }
}
