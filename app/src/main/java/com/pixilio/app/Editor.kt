package com.pixilio.app

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class Tool(val label: String) {
    PENCIL("Pensil"),
    ERASER("Penghapus"),
    FILL("Isi"),
    PICKER("Pipet"),
    LINE("Garis"),
    RECT("Kotak"),
    ELLIPSE("Elips"),
    SELECT("Pilih"),
    PAN("Geser")
}

class Layer(name: String, var pixels: IntArray) {
    var name by mutableStateOf(name)
    var visible by mutableStateOf(true)
    var opacity by mutableFloatStateOf(1f)
}

class Clip(val w: Int, val h: Int, val pixels: IntArray, val label: String) {
    val bitmap: Bitmap by lazy { Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888) }
}

class Action(val undo: () -> Unit, val redo: () -> Unit)

class Changes(val uniform: Boolean = false, val uo: Int = 0, val un: Int = 0) {
    var idx = IntArray(256)
    var old = IntArray(if (uniform) 0 else 256)
    var fresh = IntArray(if (uniform) 0 else 256)
    var n = 0

    fun add(i: Int, o: Int, nw: Int) {
        if (n == idx.size) {
            val c = n * 2
            idx = idx.copyOf(c)
            if (!uniform) {
                old = old.copyOf(c)
                fresh = fresh.copyOf(c)
            }
        }
        idx[n] = i
        if (!uniform) {
            old[n] = o
            fresh[n] = nw
        }
        n++
    }
}

/** Gabungkan satu piksel (src) di atas dst, ARGB lurus (non-premultiplied). */
fun blendOver(dst: Int, src: Int, opacity: Float): Int {
    var sa = src ushr 24
    if (opacity < 1f) sa = (sa * opacity + 0.5f).toInt()
    if (sa == 0) return dst
    val da = dst ushr 24
    if (sa == 255 || da == 0) return (sa shl 24) or (src and 0xFFFFFF)
    val ia = 255 - sa
    val outA = sa + (da * ia + 127) / 255
    if (outA == 0) return 0
    val d = outA * 255
    val r = (((src shr 16) and 255) * sa * 255 + ((dst shr 16) and 255) * da * ia + d / 2) / d
    val g = (((src shr 8) and 255) * sa * 255 + ((dst shr 8) and 255) * da * ia + d / 2) / d
    val b = ((src and 255) * sa * 255 + (dst and 255) * da * ia + d / 2) / d
    return (min(outA, 255) shl 24) or (min(r, 255) shl 16) or (min(g, 255) shl 8) or min(b, 255)
}

fun defaultPalette(): List<Int> {
    val rgb = listOf(
        0x000000, 0x1D2B53, 0x7E2553, 0x008751, 0xAB5236, 0x5F574F, 0xC2C3C7, 0xFFF1E8,
        0xFF004D, 0xFFA300, 0xFFEC27, 0x00E436, 0x29ADFF, 0x83769C, 0xFF77A8, 0xFFCCAA,
        0x291814, 0x111D35, 0x422136, 0x125359, 0x742F29, 0x49333B, 0xA28879, 0xF3EF7D,
        0xBE1250, 0xFF6C24, 0xA8E72E, 0x00B543, 0x065AB5, 0x754665, 0xFF6E59, 0xFF9D81
    )
    return rgb.map { 0xFF000000.toInt() or it }
}

class Editor {
    var width by mutableIntStateOf(32)
    var height by mutableIntStateOf(32)
    val layers = mutableStateListOf<Layer>()
    var active by mutableIntStateOf(0)
    var version by mutableIntStateOf(0)

    private var toolState by mutableStateOf(Tool.PENCIL)
    var tool: Tool
        get() = toolState
        set(value) {
            if (value == toolState) return
            // ganti tool dulu, supaya commitFloat tidak membuat ulang seleksi dari tool lama
            toolState = value
            if (floating != null) commitFloat()
            selection = null
            dragMode = 0
            clearPreview()
        }
    var color by mutableIntStateOf(0xFF000000.toInt())
    val recent = mutableStateListOf<Int>()
    val palette = mutableStateListOf<Int>()
    var brush by mutableIntStateOf(1)
    var shapeFill by mutableStateOf(false)
    var mirrorX by mutableStateOf(false)
    var mirrorY by mutableStateOf(false)
    var showGrid by mutableStateOf(true)

    var zoom by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var viewSize = IntSize.Zero

    var selection by mutableStateOf<IntArray?>(null)
    val clips = mutableStateListOf<Clip>()
    var floating by mutableStateOf<Clip?>(null)
    var floatPos by mutableStateOf(Offset.Zero)

    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)

    var bitmap: Bitmap? = null
    private var comp = IntArray(0)
    var onMessage: (String) -> Unit = {}
    var onPaletteChange: () -> Unit = {}
    private var needFit = true

    private val history = ArrayList<Action>()
    private var hp = 0

    private var pv = IntArray(1024)
    private var pvN = 0
    val pvArr: IntArray get() = pv
    val pvCount: Int get() = pvN

    private var dx0 = Int.MAX_VALUE
    private var dy0 = Int.MAX_VALUE
    private var dx1 = -1
    private var dy1 = -1

    private var cur: Changes? = null
    private var curLayer: Layer? = null

    private var dragMode = 0 // 0 none, 1 gambar, 2 bentuk, 3 pilih, 4 geser, 5 tempel
    private var lastRaw = Offset.Zero
    private var lastX = 0
    private var lastY = 0
    private var startX = 0
    private var startY = 0

    init {
        palette.addAll(defaultPalette())
    }

    fun activeLayer(): Layer? = layers.getOrNull(active)

    // ---------------------------------------------------------------- dokumen

    fun newDoc(w: Int, h: Int, pixels: IntArray? = null, name: String = "Layer 1"): Boolean {
        if (w < 1 || h < 1 || w.toLong() * h > Int.MAX_VALUE.toLong()) {
            onMessage("Ukuran tidak valid")
            return false
        }
        return try {
            val layer = Layer(name, pixels ?: IntArray(w * h))
            loadDoc(w, h, listOf(layer), 0)
            true
        } catch (e: OutOfMemoryError) {
            onMessage("Memori perangkat tidak cukup untuk ukuran ini")
            false
        }
    }

    fun loadDoc(w: Int, h: Int, ls: List<Layer>, act: Int) {
        val nb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val nc = IntArray(0)
        history.clear()
        hp = 0
        sync()
        selection = null
        floating = null
        cur = null
        resetDirty()
        pvN = 0
        width = w
        height = h
        layers.clear()
        layers.addAll(ls)
        active = act.coerceIn(0, max(layers.size - 1, 0))
        bitmap = nb
        comp = nc
        recompute(0, 0, w - 1, h - 1)
        version++
        needFit = true
        fitIfReady()
    }

    fun pixelAt(x: Int, y: Int, checker: Boolean): Int {
        val i = y * width + x
        var c = if (checker) (if (((x + y) and 1) == 0) -1 else 0xFFD0D0D0.toInt()) else 0
        for (l in layers) {
            if (!l.visible) continue
            c = blendOver(c, l.pixels[i], l.opacity)
        }
        return c
    }

    private fun recompute(ax: Int, ay: Int, bx: Int, by: Int) {
        val bmp = bitmap ?: return
        val x0 = max(ax, 0)
        val y0 = max(ay, 0)
        val x1 = min(bx, width - 1)
        val y1 = min(by, height - 1)
        if (x1 < x0 || y1 < y0) return
        val n = x1 - x0 + 1
        if (comp.size < n) comp = IntArray(n)
        for (y in y0..y1) {
            var k = 0
            for (x in x0..x1) comp[k++] = pixelAt(x, y, false)
            bmp.setPixels(comp, 0, n, x0, y, n, 1)
        }
    }

    private fun fullRefresh() {
        if (bitmap == null) return
        recompute(0, 0, width - 1, height - 1)
        resetDirty()
        version++
    }

    private fun markDirty(x: Int, y: Int) {
        if (x < dx0) dx0 = x
        if (y < dy0) dy0 = y
        if (x > dx1) dx1 = x
        if (y > dy1) dy1 = y
    }

    private fun resetDirty() {
        dx0 = Int.MAX_VALUE
        dy0 = Int.MAX_VALUE
        dx1 = -1
        dy1 = -1
    }

    private fun flush() {
        if (dx1 >= 0) {
            recompute(dx0, dy0, dx1, dy1)
            resetDirty()
            version++
        }
    }

    // ---------------------------------------------------------------- tampilan

    fun onViewSize(s: IntSize) {
        viewSize = s
        fitIfReady()
    }

    private fun fitIfReady() {
        if (needFit && viewSize.width > 0 && viewSize.height > 0) fit()
    }

    fun fit() {
        val vw = viewSize.width.toFloat()
        val vh = viewSize.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val z = (min(vw / width, vh / height) * 0.92f).coerceIn(0.02f, 128f)
        zoom = z
        offset = Offset((vw - width * z) / 2f, (vh - height * z) / 2f)
        needFit = false
    }

    fun transform(c: Offset, zc: Float, pan: Offset) {
        val nz = (zoom * zc).coerceIn(0.02f, 128f)
        val f = nz / zoom
        offset = c - (c - offset) * f + pan
        zoom = nz
    }

    // ---------------------------------------------------------------- riwayat

    private fun push(a: Action) {
        while (history.size > hp) history.removeAt(history.size - 1)
        history.add(a)
        if (history.size > 500) history.removeAt(0)
        hp = history.size
        sync()
    }

    private fun sync() {
        canUndo = hp > 0
        canRedo = hp < history.size
    }

    fun undo() {
        if (dragMode != 0 || hp <= 0) return
        hp--
        history[hp].undo()
        sync()
    }

    fun redo() {
        if (dragMode != 0 || hp >= history.size) return
        history[hp].redo()
        hp++
        sync()
    }

    // ---------------------------------------------------------------- edit piksel

    private fun begin(l: Layer, uniform: Boolean = false, uo: Int = 0, un: Int = 0) {
        cur = Changes(uniform, uo, un)
        curLayer = l
    }

    private fun put(x: Int, y: Int, c: Int) {
        val ch = cur ?: return
        val l = curLayer ?: return
        if (x < 0 || y < 0 || x >= width || y >= height) return
        val i = y * width + x
        val o = l.pixels[i]
        if (o == c) return
        ch.add(i, o, c)
        l.pixels[i] = c
        markDirty(x, y)
    }

    private fun end() {
        val ch = cur ?: return
        val l = curLayer ?: return
        cur = null
        curLayer = null
        flush()
        if (ch.n == 0) return
        push(Action({ applyChanges(l, ch, true) }, { applyChanges(l, ch, false) }))
    }

    private fun applyChanges(l: Layer, ch: Changes, undo: Boolean) {
        val w = width
        if (undo) {
            var k = ch.n - 1
            while (k >= 0) {
                val i = ch.idx[k]
                l.pixels[i] = if (ch.uniform) ch.uo else ch.old[k]
                markDirty(i % w, i / w)
                k--
            }
        } else {
            for (k in 0 until ch.n) {
                val i = ch.idx[k]
                l.pixels[i] = if (ch.uniform) ch.un else ch.fresh[k]
                markDirty(i % w, i / w)
            }
        }
        flush()
    }

    private fun plot(x: Int, y: Int, c: Int) {
        put(x, y, c)
        if (mirrorX) put(width - 1 - x, y, c)
        if (mirrorY) put(x, height - 1 - y, c)
        if (mirrorX && mirrorY) put(width - 1 - x, height - 1 - y, c)
    }

    private fun stamp(x: Int, y: Int, c: Int) {
        val n = brush
        val s = (n - 1) / 2
        for (yy in 0 until n) for (xx in 0 until n) plot(x - s + xx, y - s + yy, c)
    }

    private fun line(x0: Int, y0: Int, x1: Int, y1: Int, f: (Int, Int) -> Unit) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            f(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun shape(x0: Int, y0: Int, x1: Int, y1: Int, f: (Int, Int) -> Unit) {
        when (tool) {
            Tool.LINE -> line(x0, y0, x1, y1, f)
            Tool.RECT -> {
                val ax = min(x0, x1)
                val bx = max(x0, x1)
                val ay = min(y0, y1)
                val by = max(y0, y1)
                if (shapeFill) {
                    for (y in max(ay, 0)..min(by, height - 1)) {
                        for (x in max(ax, 0)..min(bx, width - 1)) f(x, y)
                    }
                } else {
                    line(ax, ay, bx, ay, f)
                    line(ax, by, bx, by, f)
                    line(ax, ay, ax, by, f)
                    line(bx, ay, bx, by, f)
                }
            }
            Tool.ELLIPSE -> ellipse(x0, y0, x1, y1, f)
            else -> {}
        }
    }

    private fun ellipse(x0: Int, y0: Int, x1: Int, y1: Int, f: (Int, Int) -> Unit) {
        val ax = min(x0, x1)
        val bx = max(x0, x1)
        val ay = min(y0, y1)
        val by = max(y0, y1)
        val cx = (ax + bx) / 2.0
        val cy = (ay + by) / 2.0
        val rx = (bx - ax) / 2.0 + 0.5
        val ry = (by - ay) / 2.0 + 0.5
        fun inside(x: Int, y: Int): Boolean {
            val dx = (x - cx) / rx
            val dy = (y - cy) / ry
            return dx * dx + dy * dy <= 1.0
        }
        for (y in max(ay, 0)..min(by, height - 1)) {
            for (x in max(ax, 0)..min(bx, width - 1)) {
                if (!inside(x, y)) continue
                if (shapeFill || !inside(x - 1, y) || !inside(x + 1, y) || !inside(x, y - 1) || !inside(x, y + 1)) {
                    f(x, y)
                }
            }
        }
    }

    private fun pvAdd(x: Int, y: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        if (pvN == pv.size) pv = pv.copyOf(pvN * 2)
        pv[pvN++] = y * width + x
    }

    private fun pvAddSym(x: Int, y: Int) {
        pvAdd(x, y)
        if (mirrorX) pvAdd(width - 1 - x, y)
        if (mirrorY) pvAdd(x, height - 1 - y)
        if (mirrorX && mirrorY) pvAdd(width - 1 - x, height - 1 - y)
    }

    private fun updatePreview(x: Int, y: Int) {
        pvN = 0
        lastX = x
        lastY = y
        shape(startX, startY, x, y) { px, py -> pvAddSym(px, py) }
        version++
    }

    private fun clearPreview() {
        pvN = 0
        version++
    }

    private fun commitShape() {
        val l = activeLayer()
        if (l != null) {
            begin(l)
            val c = color
            shape(startX, startY, lastX, lastY) { px, py -> plot(px, py, c) }
            end()
            touchRecent(c)
        }
        clearPreview()
    }

    private fun floodFill(x: Int, y: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        val l = activeLayer() ?: return
        val target = l.pixels[y * width + x]
        val repl = color
        if (target == repl) return
        try {
            begin(l, true, target, repl)
            val ch = cur ?: return
            var stack = IntArray(1024)
            var sp = 0
            val w = width
            val h = height
            val start = y * w + x
            l.pixels[start] = repl
            ch.add(start, target, repl)
            markDirty(x, y)
            stack[sp++] = start
            while (sp > 0) {
                val i = stack[--sp]
                val cx = i % w
                val cy = i / w
                for (d in 0 until 4) {
                    val nx = cx + (if (d == 0) -1 else if (d == 1) 1 else 0)
                    val ny = cy + (if (d == 2) -1 else if (d == 3) 1 else 0)
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val ni = ny * w + nx
                    if (l.pixels[ni] != target) continue
                    l.pixels[ni] = repl
                    ch.add(ni, target, repl)
                    markDirty(nx, ny)
                    if (sp == stack.size) stack = stack.copyOf(sp * 2)
                    stack[sp++] = ni
                }
            }
            end()
            touchRecent(repl)
        } catch (oom: OutOfMemoryError) {
            cur?.let { applyChanges(l, it, true) }
            cur = null
            curLayer = null
            onMessage("Memori tidak cukup untuk mengisi area sebesar ini")
        }
    }

    private fun pick(x: Int, y: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        val c = pixelAt(x, y, false)
        if ((c ushr 24) == 0) {
            onMessage("Piksel transparan, warna tidak diambil")
            return
        }
        useColor(c)
    }

    // ---------------------------------------------------------------- warna

    fun useColor(c: Int) {
        color = c
        touchRecent(c)
    }

    private fun touchRecent(c: Int) {
        recent.remove(c)
        recent.add(0, c)
        while (recent.size > 12) recent.removeAt(recent.size - 1)
    }

    fun addPalette(c: Int) {
        if (c !in palette) {
            palette.add(c)
            onPaletteChange()
        }
    }

    fun removePalette(c: Int) {
        if (palette.remove(c)) onPaletteChange()
    }

    // ---------------------------------------------------------------- pointer

    private fun toCell(v: Float, o: Float): Int = floor((v - o) / zoom).toInt()

    fun pointerDown(p: Offset) {
        lastRaw = p
        if (floating != null) {
            dragMode = 5
            return
        }
        val x = toCell(p.x, offset.x)
        val y = toCell(p.y, offset.y)
        when (tool) {
            Tool.PAN -> dragMode = 4
            Tool.PENCIL, Tool.ERASER -> {
                val l = activeLayer() ?: return
                begin(l)
                val c = if (tool == Tool.ERASER) 0 else color
                if (tool == Tool.PENCIL) touchRecent(color)
                stamp(x, y, c)
                lastX = x
                lastY = y
                flush()
                dragMode = 1
            }
            Tool.FILL -> floodFill(x, y)
            Tool.PICKER -> pick(x, y)
            Tool.LINE, Tool.RECT, Tool.ELLIPSE -> {
                startX = x
                startY = y
                dragMode = 2
                updatePreview(x, y)
            }
            Tool.SELECT -> {
                val cx = x.coerceIn(0, width - 1)
                val cy = y.coerceIn(0, height - 1)
                startX = cx
                startY = cy
                selection = intArrayOf(cx, cy, cx, cy)
                dragMode = 3
            }
        }
    }

    fun pointerMove(p: Offset) {
        when (dragMode) {
            5 -> {
                floatPos += (p - lastRaw) / zoom
                lastRaw = p
            }
            4 -> {
                offset += p - lastRaw
                lastRaw = p
            }
            1 -> {
                val x = toCell(p.x, offset.x)
                val y = toCell(p.y, offset.y)
                val c = if (tool == Tool.ERASER) 0 else color
                line(lastX, lastY, x, y) { ax, ay -> stamp(ax, ay, c) }
                lastX = x
                lastY = y
                flush()
            }
            2 -> updatePreview(toCell(p.x, offset.x), toCell(p.y, offset.y))
            3 -> {
                val cx = toCell(p.x, offset.x).coerceIn(0, width - 1)
                val cy = toCell(p.y, offset.y).coerceIn(0, height - 1)
                selection = intArrayOf(min(startX, cx), min(startY, cy), max(startX, cx), max(startY, cy))
            }
        }
    }

    fun pointerUp() {
        when (dragMode) {
            1 -> end()
            2 -> commitShape()
        }
        dragMode = 0
    }

    fun pointerCancel() {
        when (dragMode) {
            1 -> end()
            2 -> clearPreview()
        }
        dragMode = 0
    }

    // ---------------------------------------------------------------- seleksi & clipboard

    fun selectAll() {
        selection = intArrayOf(0, 0, width - 1, height - 1)
    }

    fun clearSelection() {
        selection = null
    }

    fun copySelection(cut: Boolean, merged: Boolean = false) {
        val s = selection ?: run {
            onMessage("Pilih area dulu dengan alat Pilih")
            return
        }
        val l = activeLayer() ?: return
        val w = s[2] - s[0] + 1
        val h = s[3] - s[1] + 1
        try {
            val buf = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    buf[y * w + x] = if (merged) pixelAt(s[0] + x, s[1] + y, false)
                    else l.pixels[(s[1] + y) * width + s[0] + x]
                }
            }
            val tag = if (merged) " (gabungan)" else ""
            clips.add(0, Clip(w, h, buf, "$w×$h$tag"))
            if (cut) clearRect(s)
            onMessage(if (cut) "Dipotong ke clipboard" else "Disalin ke clipboard")
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    private fun clearRect(s: IntArray) {
        val l = activeLayer() ?: return
        begin(l)
        for (y in s[1]..s[3]) for (x in s[0]..s[2]) put(x, y, 0)
        end()
    }

    fun deleteSelection() {
        val s = selection ?: return
        clearRect(s)
    }

    fun paste(c: Clip) {
        if (floating != null) commitFloat()
        if (toolState != Tool.SELECT) {
            // tempel selalu terjadi di mode Pilih, supaya tidak "bocor" ke tool lain
            toolState = Tool.SELECT
            dragMode = 0
            clearPreview()
        }
        floating = c
        val s = selection
        floatPos = if (s != null) {
            Offset(s[0].toFloat(), s[1].toFloat())
        } else {
            val cx = (viewSize.width / 2f - offset.x) / zoom - c.w / 2f
            val cy = (viewSize.height / 2f - offset.y) / zoom - c.h / 2f
            Offset(floor(cx), floor(cy))
        }
    }

    fun commitFloat() {
        val c = floating ?: return
        val l = activeLayer()
        if (l == null) {
            floating = null
            return
        }
        val fx = floor(floatPos.x).toInt()
        val fy = floor(floatPos.y).toInt()
        floating = null
        begin(l)
        for (yy in 0 until c.h) {
            val ty = fy + yy
            if (ty < 0 || ty >= height) continue
            for (xx in 0 until c.w) {
                val col = c.pixels[yy * c.w + xx]
                if ((col ushr 24) == 0) continue
                put(fx + xx, ty, col)
            }
        }
        end()
        val x0 = max(fx, 0)
        val y0 = max(fy, 0)
        val x1 = min(fx + c.w - 1, width - 1)
        val y1 = min(fy + c.h - 1, height - 1)
        selection = if (tool == Tool.SELECT && x1 >= x0 && y1 >= y0) intArrayOf(x0, y0, x1, y1) else null
        version++
    }

    fun cancelFloat() {
        floating = null
        version++
    }

    // ---------------------------------------------------------------- layer

    private fun insertLayer(l: Layer) {
        commitFloat()
        val idx = active + 1
        val prev = active
        layers.add(idx, l)
        active = idx
        fullRefresh()
        push(
            Action(
                { layers.remove(l); active = prev.coerceIn(0, layers.size - 1); fullRefresh() },
                { layers.add(idx, l); active = idx; fullRefresh() }
            )
        )
    }

    fun addLayer() {
        try {
            insertLayer(Layer("Layer ${layers.size + 1}", IntArray(width * height)))
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    fun duplicateLayer() {
        val src = activeLayer() ?: return
        try {
            val l = Layer(src.name + " salinan", src.pixels.copyOf())
            l.opacity = src.opacity
            l.visible = src.visible
            insertLayer(l)
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    fun deleteLayer() {
        if (layers.size <= 1) {
            onMessage("Minimal harus ada satu layer")
            return
        }
        commitFloat()
        val idx = active
        val l = layers[idx]
        layers.removeAt(idx)
        active = min(idx, layers.size - 1)
        fullRefresh()
        push(
            Action(
                { layers.add(idx, l); active = idx; fullRefresh() },
                { layers.remove(l); active = min(idx, layers.size - 1); fullRefresh() }
            )
        )
    }

    private fun swap(i: Int, j: Int) {
        val a = layers[i]
        layers[i] = layers[j]
        layers[j] = a
    }

    /** dir = +1 naik (ke atas), -1 turun. */
    fun moveLayer(dir: Int) {
        val i = active
        val j = i + dir
        if (j !in layers.indices) return
        commitFloat()
        swap(i, j)
        active = j
        fullRefresh()
        push(
            Action(
                { swap(i, j); active = i; fullRefresh() },
                { swap(i, j); active = j; fullRefresh() }
            )
        )
    }

    fun mergeDown() {
        val i = active
        if (i <= 0) {
            onMessage("Tidak ada layer di bawahnya")
            return
        }
        commitFloat()
        try {
            val top = layers[i]
            val below = layers[i - 1]
            val oldBelow = below.pixels
            val merged = IntArray(oldBelow.size)
            for (k in merged.indices) {
                merged[k] = if (top.visible) blendOver(oldBelow[k], top.pixels[k], top.opacity) else oldBelow[k]
            }
            below.pixels = merged
            layers.removeAt(i)
            active = i - 1
            fullRefresh()
            push(
                Action(
                    { below.pixels = oldBelow; layers.add(i, top); active = i; fullRefresh() },
                    { below.pixels = merged; layers.remove(top); active = i - 1; fullRefresh() }
                )
            )
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    fun setVisible(l: Layer, v: Boolean) {
        l.visible = v
        fullRefresh()
    }

    fun setOpacity(l: Layer, o: Float) {
        l.opacity = o
        fullRefresh()
    }

    // ---------------------------------------------------------------- transformasi

    private fun replacePixels(l: Layer, n: IntArray) {
        val old = l.pixels
        l.pixels = n
        fullRefresh()
        push(Action({ l.pixels = old; fullRefresh() }, { l.pixels = n; fullRefresh() }))
    }

    fun flipLayer(horizontal: Boolean) {
        val l = activeLayer() ?: return
        commitFloat()
        try {
            val old = l.pixels
            val n = IntArray(old.size)
            val w = width
            val h = height
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val sx = if (horizontal) w - 1 - x else x
                    val sy = if (horizontal) y else h - 1 - y
                    n[y * w + x] = old[sy * w + sx]
                }
            }
            replacePixels(l, n)
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    fun clearLayer() {
        val l = activeLayer() ?: return
        commitFloat()
        try {
            replacePixels(l, IntArray(l.pixels.size))
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    private fun setDoc(w: Int, h: Int, arrs: List<IntArray>) {
        val nb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val nc = IntArray(0)
        for (i in layers.indices) layers[i].pixels = arrs[i]
        width = w
        height = h
        bitmap = nb
        comp = nc
        selection = null
        pvN = 0
        resetDirty()
        recompute(0, 0, w - 1, h - 1)
        version++
        needFit = true
        fitIfReady()
    }

    fun rotate90() {
        commitFloat()
        val ow = width
        val oh = height
        try {
            val olds = layers.map { it.pixels }
            val news = olds.map { old ->
                val n = IntArray(old.size)
                for (y in 0 until oh) for (x in 0 until ow) n[x * oh + (oh - 1 - y)] = old[y * ow + x]
                n
            }
            setDoc(oh, ow, news)
            push(Action({ setDoc(ow, oh, olds) }, { setDoc(oh, ow, news) }))
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori tidak cukup")
        }
    }

    fun resizeCanvas(nw: Int, nh: Int): Boolean {
        if (nw < 1 || nh < 1 || nw.toLong() * nh > Int.MAX_VALUE.toLong()) {
            onMessage("Ukuran tidak valid")
            return false
        }
        if (nw == width && nh == height) return true
        commitFloat()
        val ow = width
        val oh = height
        return try {
            val olds = layers.map { it.pixels }
            val news = olds.map { old ->
                val n = IntArray(nw * nh)
                val cw = min(ow, nw)
                val ch = min(oh, nh)
                for (y in 0 until ch) System.arraycopy(old, y * ow, n, y * nw, cw)
                n
            }
            setDoc(nw, nh, news)
            push(Action({ setDoc(ow, oh, olds) }, { setDoc(nw, nh, news) }))
            true
        } catch (oom: OutOfMemoryError) {
            onMessage("Memori perangkat tidak cukup untuk ukuran ini")
            false
        }
    }
}
