package com.pixilio.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Ukuran kotak latar (px layar), statis: tidak ikut zoom / ukuran piksel gambar. */
object Bg {
    val options = listOf(16, 32, 64, 128, 256)
    var size by mutableIntStateOf(32)
        private set
    var onChange: (() -> Unit)? = null

    fun load(v: Int) {
        size = if (v in options) v else 32
    }

    fun set(v: Int) {
        load(v)
        onChange?.invoke()
    }
}

/** Gambar latar kotak-kotak putih/abu di area (left, top, w, h), dipotong ke layar. */
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

@Composable
fun BgDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Latar kotak-kotak") },
        text = {
            Column {
                Bg.options.forEach { v ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { Bg.set(v) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = Bg.size == v, onClick = { Bg.set(v) })
                        Text("$v px")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
