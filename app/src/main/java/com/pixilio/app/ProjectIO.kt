package com.pixilio.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class DocSnapshot(
    val w: Int,
    val h: Int,
    val names: List<String>,
    val vis: List<Boolean>,
    val op: List<Float>,
    val pix: List<IntArray>,
    val act: Int
)

class ImageData(val w: Int, val h: Int, val px: IntArray)

object ProjectIO {
    private const val MAGIC = 0x50584C4F // "PXLO"
    private const val FORMAT = 1
    private const val CHUNK = 65536

    fun snapshot(e: Editor): DocSnapshot = DocSnapshot(
        e.width, e.height,
        e.layers.map { it.name },
        e.layers.map { it.visible },
        e.layers.map { it.opacity },
        e.layers.map { it.pixels.copyOf() },
        e.active
    )

    fun write(s: DocSnapshot, out: OutputStream) {
        val d = DataOutputStream(BufferedOutputStream(GZIPOutputStream(out), 1 shl 16))
        d.use {
            it.writeInt(MAGIC)
            it.writeInt(FORMAT)
            it.writeInt(s.w)
            it.writeInt(s.h)
            it.writeInt(s.pix.size)
            it.writeInt(s.act)
            for (i in s.pix.indices) {
                it.writeUTF(s.names[i])
                it.writeBoolean(s.vis[i])
                it.writeFloat(s.op[i])
                writeInts(it, s.pix[i])
            }
        }
    }

    fun read(input: InputStream): DocSnapshot {
        val d = DataInputStream(BufferedInputStream(GZIPInputStream(input), 1 shl 16))
        d.use { din ->
            if (din.readInt() != MAGIC) throw IOException("Format file tidak dikenali")
            din.readInt() // versi format
            val w = din.readInt()
            val h = din.readInt()
            val n = din.readInt()
            val act = din.readInt()
            if (w < 1 || h < 1 || n < 1 || w.toLong() * h > Int.MAX_VALUE.toLong()) {
                throw IOException("File proyek rusak")
            }
            val names = ArrayList<String>()
            val vis = ArrayList<Boolean>()
            val op = ArrayList<Float>()
            val pix = ArrayList<IntArray>()
            repeat(n) {
                names.add(din.readUTF())
                vis.add(din.readBoolean())
                op.add(din.readFloat())
                val arr = IntArray(w * h)
                readInts(din, arr)
                pix.add(arr)
            }
            return DocSnapshot(w, h, names, vis, op, pix, act)
        }
    }

    fun apply(e: Editor, s: DocSnapshot) {
        val ls = s.pix.indices.map { i ->
            Layer(s.names[i], s.pix[i]).also {
                it.visible = s.vis[i]
                it.opacity = s.op[i]
            }
        }
        e.loadDoc(s.w, s.h, ls, s.act)
    }

    fun decodeImage(input: InputStream): ImageData? {
        val bmp = BitmapFactory.decodeStream(input) ?: return null
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        return ImageData(w, h, px)
    }

    fun compositePixels(e: Editor): IntArray {
        val w = e.width
        val h = e.height
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) px[y * w + x] = e.pixelAt(x, y, false)
        return px
    }

    fun writePng(px: IntArray, w: Int, h: Int, scale: Int, out: OutputStream) {
        val base = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val bmp = if (scale > 1) Bitmap.createScaledBitmap(base, w * scale, h * scale, false) else base
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
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
