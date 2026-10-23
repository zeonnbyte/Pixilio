package com.pixilio.app

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Menyimpan riwayat clipboard ke filesDir supaya tidak hilang saat aplikasi
 * ditutup, di-update, atau prosesnya dimatikan sistem.
 */
object ClipStore {
    private const val MAGIC = 0x50584343 // "PXCC"
    private const val VERSION = 1
    private const val CHUNK = 1 shl 14
    private val lock = Any()

    fun file(filesDir: File): File = File(filesDir, "clipboard.pxc")

    fun save(f: File, clips: List<Clip>) {
        synchronized(lock) {
            try {
                if (clips.isEmpty()) {
                    f.delete()
                    return
                }
                val tmp = File(f.parentFile, f.name + ".tmp")
                DataOutputStream(BufferedOutputStream(GZIPOutputStream(FileOutputStream(tmp)), 1 shl 16)).use { d ->
                    d.writeInt(MAGIC)
                    d.writeInt(VERSION)
                    d.writeInt(clips.size)
                    for (c in clips) {
                        d.writeInt(c.w)
                        d.writeInt(c.h)
                        d.writeUTF(c.label)
                        writeInts(d, c.pixels)
                    }
                }
                if (!tmp.renameTo(f)) {
                    f.delete()
                    tmp.renameTo(f)
                }
            } catch (t: Throwable) {
                // gagal simpan clipboard tidak boleh membuat aplikasi crash
            }
        }
    }

    fun load(f: File): List<Clip> {
        synchronized(lock) {
            if (!f.exists()) return emptyList()
            return try {
                DataInputStream(BufferedInputStream(GZIPInputStream(FileInputStream(f)), 1 shl 16)).use { d ->
                    if (d.readInt() != MAGIC) return emptyList()
                    if (d.readInt() != VERSION) return emptyList()
                    val n = d.readInt()
                    val out = ArrayList<Clip>(n)
                    for (i in 0 until n) {
                        val w = d.readInt()
                        val h = d.readInt()
                        val label = d.readUTF()
                        if (w <= 0 || h <= 0 || w.toLong() * h > Int.MAX_VALUE / 4) return out
                        val px = IntArray(w * h)
                        readInts(d, px)
                        out.add(Clip(w, h, px, label))
                    }
                    out
                }
            } catch (t: Throwable) {
                emptyList()
            }
        }
    }

    private fun writeInts(d: DataOutputStream, arr: IntArray) {
        val bb = ByteBuffer.allocate(CHUNK * 4)
        var i = 0
        while (i < arr.size) {
            val n = minOf(CHUNK, arr.size - i)
            bb.clear()
            bb.asIntBuffer().put(arr, i, n)
            d.write(bb.array(), 0, n * 4)
            i += n
        }
    }

    private fun readInts(d: DataInputStream, arr: IntArray) {
        val buf = ByteArray(CHUNK * 4)
        var i = 0
        while (i < arr.size) {
            val n = minOf(CHUNK, arr.size - i)
            d.readFully(buf, 0, n * 4)
            ByteBuffer.wrap(buf, 0, n * 4).asIntBuffer().get(arr, i, n)
            i += n
        }
    }
}
