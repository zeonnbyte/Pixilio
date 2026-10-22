package com.pixilio.app

import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
private fun ChSlider(label: String, value: Float, max: Float, shown: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(22.dp))
        Slider(
            value = value.coerceIn(0f, max),
            onValueChange = onChange,
            valueRange = 0f..max,
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        )
        Text(shown, modifier = Modifier.width(48.dp), fontSize = 12.sp)
    }
}

@Composable
private fun HsWheel(hue: Float, sat: Float, value: Float, modifier: Modifier, onChange: (Float, Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    val hueColors = remember { (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) } }
    Canvas(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun hit(p: Offset) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = p.x - cx
                    val dy = p.y - cy
                    var ang = atan2(dy, dx) * 180f / PI.toFloat()
                    if (ang < 0f) ang += 360f
                    cb(ang, (hypot(dx, dy) / min(cx, cy)).coerceIn(0f, 1f))
                }
                down.consume()
                hit(down.position)
                do {
                    val ev = awaitPointerEvent()
                    val ch = ev.changes.firstOrNull { it.pressed }
                    if (ch != null) {
                        hit(ch.position)
                        ch.consume()
                    }
                } while (ev.changes.any { it.pressed })
            }
        }
    ) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(brush = Brush.sweepGradient(hueColors, c), radius = r, center = c)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), c, r),
            radius = r,
            center = c
        )
        drawCircle(color = Color.Black.copy(alpha = 1f - value.coerceIn(0f, 1f)), radius = r, center = c)
        drawCircle(color = Color.White, radius = r, center = c, style = Stroke(2f))
        val a = hue * PI / 180.0
        val pos = Offset((c.x + cos(a) * sat * r).toFloat(), (c.y + sin(a) * sat * r).toFloat())
        drawCircle(color = Color.Black, radius = 12f, center = pos, style = Stroke(5f))
        drawCircle(color = Color.White, radius = 12f, center = pos, style = Stroke(3f))
    }
}

@Composable
fun ColorDialog(e: Editor, onDismiss: () -> Unit) {
    val start = e.color
    val init = remember { FloatArray(3).also { AColor.colorToHSV(start, it) } }
    var argb by remember { mutableIntStateOf(start) }
    var h by remember { mutableFloatStateOf(init[0]) }
    var s by remember { mutableFloatStateOf(init[1]) }
    var v by remember { mutableFloatStateOf(init[2]) }
    val initHsl = remember { ColorCodes.toHsl(start) }
    var hls by remember { mutableFloatStateOf(initHsl[1]) }
    var hll by remember { mutableFloatStateOf(initHsl[2]) }
    var mode by remember { mutableIntStateOf(0) }
    var fmt by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf(ColorCodes.format(start, 0)) }
    var bad by remember { mutableStateOf(false) }

    fun syncHsl(c: Int) {
        val x = ColorCodes.toHsl(c)
        hll = x[2]
        if (x[2] > 0f && x[2] < 1f) hls = x[1]
    }

    fun syncHsv(c: Int) {
        val t = FloatArray(3)
        AColor.colorToHSV(c, t)
        if (t[2] > 0f) {
            s = t[1]
            if (t[1] > 0f) h = t[0]
        }
        v = t[2]
    }

    fun setArgb(c: Int, fromText: Boolean = false) {
        argb = c
        syncHsv(c)
        syncHsl(c)
        if (!fromText) {
            text = ColorCodes.format(c, fmt)
            bad = false
        }
    }

    fun setHsv(nh: Float, ns: Float, nv: Float) {
        h = nh
        s = ns
        v = nv
        argb = ColorCodes.fromHsv(nh, ns, nv, argb ushr 24)
        syncHsl(argb)
        text = ColorCodes.format(argb, fmt)
        bad = false
    }

    // HSL disimpan sebagai state sendiri supaya slider tidak "melayang" akibat pembulatan 8-bit
    fun setHsl(nh: Float, ns: Float, nl: Float) {
        h = nh
        hls = ns
        hll = nl
        argb = ColorCodes.fromHsl(nh, ns, nl, argb ushr 24)
        val t = FloatArray(3)
        AColor.colorToHSV(argb, t)
        s = t[1]
        v = t[2]
        text = ColorCodes.format(argb, fmt)
        bad = false
    }

    val alpha = argb ushr 24

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Pilih warna", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Wheel") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Biasa") })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(Color(0xFF888888))
                            .background(Color(start))
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(Color(0xFF888888))
                            .background(Color(argb))
                    )
                }

                if (mode == 0) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        HsWheel(h, s, v, Modifier.size(240.dp)) { nh, ns -> setHsv(nh, ns, v) }
                    }
                    ChSlider("V", v * 100f, 100f, "${(v * 100f).roundToInt()}%") { setHsv(h, s, it / 100f) }
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorCodes.names.forEachIndexed { i, n ->
                            FilterChip(
                                selected = fmt == i,
                                onClick = {
                                    fmt = i
                                    text = ColorCodes.format(argb, i)
                                    bad = false
                                },
                                label = { Text(n) }
                            )
                        }
                    }
                    when (fmt) {
                        2 -> {
                            ChSlider("H", h, 360f, "${h.roundToInt()}°") { setHsl(it, hls, hll) }
                            ChSlider("S", hls * 100f, 100f, "${(hls * 100f).roundToInt()}%") { setHsl(h, it / 100f, hll) }
                            ChSlider("L", hll * 100f, 100f, "${(hll * 100f).roundToInt()}%") { setHsl(h, hls, it / 100f) }
                        }
                        3 -> {
                            ChSlider("H", h, 360f, "${h.roundToInt()}°") { setHsv(it, s, v) }
                            ChSlider("S", s * 100f, 100f, "${(s * 100f).roundToInt()}%") { setHsv(h, it / 100f, v) }
                            ChSlider("V", v * 100f, 100f, "${(v * 100f).roundToInt()}%") { setHsv(h, s, it / 100f) }
                        }
                        4 -> {
                            val k = ColorCodes.toCmyk(argb)
                            ChSlider("C", k[0] * 100f, 100f, "${(k[0] * 100f).roundToInt()}%") {
                                setArgb(ColorCodes.fromCmyk(it / 100f, k[1], k[2], k[3], alpha))
                            }
                            ChSlider("M", k[1] * 100f, 100f, "${(k[1] * 100f).roundToInt()}%") {
                                setArgb(ColorCodes.fromCmyk(k[0], it / 100f, k[2], k[3], alpha))
                            }
                            ChSlider("Y", k[2] * 100f, 100f, "${(k[2] * 100f).roundToInt()}%") {
                                setArgb(ColorCodes.fromCmyk(k[0], k[1], it / 100f, k[3], alpha))
                            }
                            ChSlider("K", k[3] * 100f, 100f, "${(k[3] * 100f).roundToInt()}%") {
                                setArgb(ColorCodes.fromCmyk(k[0], k[1], k[2], it / 100f, alpha))
                            }
                        }
                        else -> {
                            val r = (argb shr 16) and 255
                            val g = (argb shr 8) and 255
                            val b = argb and 255
                            ChSlider("R", r.toFloat(), 255f, "$r") { setArgb((argb and 0xFF00FFFF.toInt()) or (it.roundToInt() shl 16)) }
                            ChSlider("G", g.toFloat(), 255f, "$g") { setArgb((argb and 0xFFFF00FF.toInt()) or (it.roundToInt() shl 8)) }
                            ChSlider("B", b.toFloat(), 255f, "$b") { setArgb((argb and 0xFFFFFF00.toInt()) or it.roundToInt()) }
                        }
                    }
                }

                ChSlider("A", alpha.toFloat(), 255f, "$alpha") {
                    setArgb((argb and 0x00FFFFFF) or (it.roundToInt() shl 24))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { t ->
                        text = t
                        val c = ColorCodes.parse(t)
                        if (c == null) {
                            bad = true
                        } else {
                            bad = false
                            setArgb(c, true)
                        }
                    },
                    label = { Text("Kode warna") },
                    supportingText = { Text("HEX, RGB, HSL, HSV, CMYK, 0xAARRGGBB", fontSize = 11.sp) },
                    isError = bad,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { e.useColor(argb); e.addPalette(argb); onDismiss() }) {
                    Text("+ Simpan ke palet & pakai")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    TextButton(onClick = { e.useColor(argb); onDismiss() }) { Text("Pakai") }
                }
            }
        }
    }
}
