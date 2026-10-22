package com.pixilio.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.roundToInt

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

    fun snapshot(e: Editor, copy: Boolean = true): DocSnapshot = DocSnapshot(
        e.width, e.height,
        e.layers.map { it.name },
        e.layers.map { it.visible },
        e.layers.map { it.opacity },
        e.layers.map { if (copy) it.pixels.copyOf() else it.pixels },
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

    /** Thumbnail kecil (maks 256 px) dari snapshot, di-sampling nearest tanpa komposit penuh. */
    fun makeThumb(s: DocSnapshot): Bitmap {
        val m = maxOf(s.w, s.h)
        val tw: Int
        val th: Int
        if (m <= 256) {
            tw = s.w
            th = s.h
        } else {
            val f = 256f / m
            tw = (s.w * f).roundToInt().coerceAtLeast(1)
            th = (s.h * f).roundToInt().coerceAtLeast(1)
        }
        val out = IntArray(tw * th)
        for (ty in 0 until th) {
            val sy = (ty.toLong() * s.h / th).toInt()
            for (tx in 0 until tw) {
                val sx = (tx.toLong() * s.w / tw).toInt()
                val i = sy * s.w + sx
                var c = 0
                for (k in s.pix.indices) {
                    if (!s.vis[k]) continue
                    c = blendOver(c, s.pix[k][i], s.op[k])
                }
                out[ty * tw + tx] = c
            }
        }
        val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        bmp.setPremultiplied(false)
        bmp.setPixels(out, 0, tw, 0, 0, tw, th)
        return bmp
    }

    fun writeThumb(s: DocSnapshot, t: File) {
        val bmp = makeThumb(s)
        FileOutputStream(t).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Muat thumbnail proyek; kalau belum ada, dibuat dari file proyek. */
    fun loadThumb(f: File): Bitmap? {
        val t = ProjectStore.thumb(f)
        return try {
            if (t.exists()) BitmapFactory.decodeFile(t.path)?.let { return it }
            val s = FileInputStream(f).use { read(it) }
            writeThumb(s, t)
            BitmapFactory.decodeFile(t.path)
        } catch (x: Throwable) {
            null
        }
    }

    fun decodeImage(input: InputStream): ImageData? {
        val opts = BitmapFactory.Options().apply {
            inPremultiplied = false
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeStream(input, null, opts) ?: return null
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        return ImageData(w, h, px)
    }

    /** Baca ukuran kanvas saja dari file proyek (tanpa memuat piksel). */
    fun readSize(f: File): Pair<Int, Int>? {
        val r: Pair<Int, Int>? = try {
            DataInputStream(BufferedInputStream(GZIPInputStream(FileInputStream(f)), 1 shl 12)).use { d ->
                val ok = d.readInt() == MAGIC
                d.readInt()
                val w = d.readInt()
                val h = d.readInt()
                if (!ok || w < 1 || h < 1) null else Pair(w, h)
            }
        } catch (x: Throwable) {
            null
        }
        return r
    }

    /** Gabungkan semua layer yang terlihat dari snapshot menjadi satu gambar (sama dengan tampilan editor). */
    fun flatten(s: DocSnapshot): IntArray {
        val out = IntArray(s.w * s.h)
        for (li in s.pix.indices) {
            if (!s.vis[li]) continue
            val p = s.pix[li]
            val o = s.op[li]
            for (i in out.indices) out[i] = blendOver(out[i], p[i], o)
        }
        return out
    }

    fun compositePixels(e: Editor): IntArray {
        val w = e.width
        val h = e.height
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) px[y * w + x] = e.pixelAt(x, y, false)
        return px
    }

    fun writePng(px: IntArray, w: Int, h: Int, scale: Int, out: OutputStream) {
        val sw = w * scale
        val sh = h * scale
        val data = if (scale > 1) {
            val a = IntArray(sw * sh)
            for (y in 0 until sh) {
                val src = (y / scale) * w
                val row = y * sw
                for (x in 0 until sw) a[row + x] = px[src + x / scale]
            }
            a
        } else px
        // non-premultiplied: warna semi-transparan tersimpan persis, tanpa pembulatan
        val bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        bmp.setPremultiplied(false)
        bmp.setPixels(data, 0, sw, 0, 0, sw, sh)
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
