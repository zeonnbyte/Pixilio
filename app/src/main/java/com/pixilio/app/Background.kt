package com.pixilio.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 1 kotak latar = 16 piksel kanvas (kanvas 16 = polos, 32 = 2x2, 64 = 4x4, dst). */
const val CHECKER_PX = 16

/** Gambar latar kotak-kotak putih/abu di area (left, top, w, h), dipotong ke layar. cell = ukuran kotak di layar. */
fun DrawScope.drawChecker(left: Float, top: Float, w: Float, h: Float, cell: Float) {
    val l = max(left, 0f)
    val t = max(top, 0f)
    val r = min(left + w, size.width)
    val b = min(top + h, size.height)
    if (r <= l || b <= t || cell <= 0f) return
    drawRect(Color.White, Offset(l, t), Size(r - l, b - t))
    val dark = Color(0xFFD0D0D0)
    val c0 = floor((l - left) / cell).toInt()
    val c1 = ceil((r - left) / cell).toInt()
    val r0 = floor((t - top) / cell).toInt()
    val r1 = ceil((b - top) / cell).toInt()
    for (cy in r0 until r1) {
        for (cx in c0 until c1) {
            if (((cx + cy) and 1) == 0) continue
            val x0 = max(left + cx * cell, l)
            val y0 = max(top + cy * cell, t)
            val x1 = min(left + (cx + 1) * cell, r)
            val y1 = min(top + (cy + 1) * cell, b)
            if (x1 > x0 && y1 > y0) drawRect(dark, Offset(x0, y0), Size(x1 - x0, y1 - y0))
        }
    }
}
