package com.pixilio.app

import android.graphics.Color as AColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Konversi & parser semua sistem kode warna: HEX, RGB(A), HSL(A), HSV/HSB, CMYK, 0xAARRGGBB. */
object ColorCodes {
    val names = listOf("HEX", "RGB", "HSL", "HSV", "CMYK", "ARGB")

    private fun f(fmt: String, vararg a: Any): String = String.format(Locale.ROOT, fmt, *a)
    private fun pct(x: Float): Int = (x * 100f).roundToInt()
    private fun alphaStr(c: Int): String = f("%.2f", (c ushr 24) / 255f)
    private fun isHex(ch: Char): Boolean = ch in '0'..'9' || ch in 'a'..'f'
    private fun to255(v: Float): Int = (v * 255f).roundToInt().coerceIn(0, 255)
    private fun pack(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun toHsv(c: Int): FloatArray = FloatArray(3).also { AColor.colorToHSV(c, it) }

    fun fromHsv(h: Float, s: Float, v: Float, a: Int): Int =
        AColor.HSVToColor(a, floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

    fun toHsl(c: Int): FloatArray {
        val r = ((c shr 16) and 255) / 255f
        val g = ((c shr 8) and 255) / 255f
        val b = (c and 255) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return floatArrayOf(if (h < 0f) h + 360f else h, s.coerceIn(0f, 1f), l)
    }

    fun fromHsl(h: Float, s: Float, l: Float, a: Int): Int {
        val sat = s.coerceIn(0f, 1f)
        val lig = l.coerceIn(0f, 1f)
        val c = (1f - abs(2f * lig - 1f)) * sat
        val hh = ((h % 360f) + 360f) % 360f
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = lig - c / 2f
        val t = when ((hh / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return pack(a, to255(t.first + m), to255(t.second + m), to255(t.third + m))
    }

    fun toCmyk(c: Int): FloatArray {
        val r = ((c shr 16) and 255) / 255f
        val g = ((c shr 8) and 255) / 255f
        val b = (c and 255) / 255f
        val k = 1f - max(r, max(g, b))
        if (k >= 1f) return floatArrayOf(0f, 0f, 0f, 1f)
        return floatArrayOf((1f - r - k) / (1f - k), (1f - g - k) / (1f - k), (1f - b - k) / (1f - k), k)
    }

    fun fromCmyk(c: Float, m: Float, y: Float, k: Float, a: Int): Int {
        val kk = 1f - k.coerceIn(0f, 1f)
        return pack(
            a,
            to255((1f - c.coerceIn(0f, 1f)) * kk),
            to255((1f - m.coerceIn(0f, 1f)) * kk),
            to255((1f - y.coerceIn(0f, 1f)) * kk)
        )
    }

    fun format(c: Int, idx: Int): String {
        val a = c ushr 24
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val b = c and 255
        val al = if (a == 255) "" else ", " + alphaStr(c)
        return when (idx) {
            0 -> if (a == 255) f("#%02X%02X%02X", r, g, b) else f("#%02X%02X%02X%02X", r, g, b, a)
            1 -> if (a == 255) "rgb($r, $g, $b)" else "rgba($r, $g, $b$al)"
            2 -> {
                val x = toHsl(c)
                "${if (a == 255) "hsl" else "hsla"}(${x[0].roundToInt() % 360}, ${pct(x[1])}%, ${pct(x[2])}%$al)"
            }
            3 -> {
                val x = toHsv(c)
                "${if (a == 255) "hsv" else "hsva"}(${x[0].roundToInt() % 360}, ${pct(x[1])}%, ${pct(x[2])}%$al)"
            }
            4 -> {
                val x = toCmyk(c)
                "cmyk(${pct(x[0])}%, ${pct(x[1])}%, ${pct(x[2])}%, ${pct(x[3])}%$al)"
            }
            else -> f("0x%08X", c)
        }
    }

    private fun parseHex(s: String): Int? {
        if (!s.all { isHex(it) }) return null
        fun d(i: Int): Int = s[i].digitToInt(16)
        fun p(i: Int): Int = s.substring(i, i + 2).toInt(16)
        return when (s.length) {
            3 -> pack(255, d(0) * 17, d(1) * 17, d(2) * 17)
            4 -> pack(d(3) * 17, d(0) * 17, d(1) * 17, d(2) * 17)
            6 -> pack(255, p(0), p(2), p(4))
            8 -> pack(p(6), p(0), p(2), p(4))
            else -> null
        }
    }

    private class Num(val v: Double, val pct: Boolean)

    private fun alphaOf(n: List<Num>, i: Int): Int {
        if (n.size <= i) return 255
        val x = n[i]
        val v = if (x.pct) x.v / 100.0 * 255.0 else if (x.v <= 1.0) x.v * 255.0 else x.v
        return v.coerceIn(0.0, 255.0).roundToInt()
    }

    private fun ch(x: Num): Int =
        (if (x.pct) x.v / 100.0 * 255.0 else x.v).coerceIn(0.0, 255.0).roundToInt()

    private fun frac(x: Num): Float = (x.v / 100.0).toFloat()

    /** Terima: #RGB #RGBA #RRGGBB #RRGGBBAA, 0xAARRGGBB, rgb()/rgba(), hsl()/hsla(), hsv()/hsb(), cmyk(), atau "255, 0, 0". */
    fun parse(raw: String): Int? {
        val t = raw.trim().lowercase(Locale.ROOT)
        if (t.isEmpty()) return null
        if (t.startsWith("0x")) {
            val h = t.substring(2)
            if ((h.length != 6 && h.length != 8) || !h.all { isHex(it) }) return null
            val v = h.toLong(16).toInt()
            return if (h.length == 6) v or (0xFF shl 24) else v
        }
        val body = t.removePrefix("#")
        if (t.startsWith("#") || (body.all { isHex(it) } && body.length in listOf(3, 4, 6, 8))) {
            return parseHex(body)
        }
        val name = Regex("^[a-z]+").find(t)?.value ?: ""
        val rest = t.substring(name.length)
        val n = Regex("(-?\\d*\\.?\\d+)\\s*(%?)").findAll(rest).map { Num(it.groupValues[1].toDouble(), it.groupValues[2] == "%") }.toList()
        return when (name) {
            "", "rgb", "rgba" -> if (n.size >= 3) pack(
                alphaOf(n, 3), ch(n[0]), ch(n[1]), ch(n[2])
            ) else null
            "hsl", "hsla" -> if (n.size >= 3) fromHsl(
                n[0].v.toFloat(), frac(n[1]), frac(n[2]), alphaOf(n, 3)
            ) else null
            "hsv", "hsva", "hsb", "hsba" -> if (n.size >= 3) fromHsv(
                n[0].v.toFloat(), frac(n[1]), frac(n[2]), alphaOf(n, 3)
            ) else null
            "cmyk" -> if (n.size >= 4) fromCmyk(
                frac(n[0]), frac(n[1]), frac(n[2]), frac(n[3]), alphaOf(n, 4)
            ) else null
            else -> null
        }
    }
}
