package com.pixilio.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Actions(
    val openProject: () -> Unit,
    val saveProject: () -> Unit,
    val exportPng: (Int) -> Unit,
    val importImage: () -> Unit,
    val version: String
)

@Composable
fun App(e: Editor, a: Actions, name: String, onHome: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var dlg by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .systemBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHome) { Icon(Lu.ChevronLeft, null, tint = Color.White) }
            Text(
                name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { e.undo() }, enabled = e.canUndo) {
                Icon(Lu.Undo2, null, tint = if (e.canUndo) Color.White else Color(0x55FFFFFF))
            }
            IconButton(onClick = { e.redo() }, enabled = e.canRedo) {
                Icon(Lu.Redo2, null, tint = if (e.canRedo) Color.White else Color(0x55FFFFFF))
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Lu.EllipsisVertical, null, tint = Color.White) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    MItem("Beranda") { menu = false; onHome() }
                    MItem("Kanvas baru (ganti isi)") { menu = false; dlg = "new" }
                    MItem("Impor file .pixilio (ganti isi)") { menu = false; a.openProject() }
                    MItem("Ekspor file .pixilio") { menu = false; a.saveProject() }
                    MItem("Ekspor PNG") { menu = false; dlg = "export" }
                    MItem("Impor gambar") { menu = false; a.importImage() }
                    MItem("Ubah ukuran kanvas") { menu = false; dlg = "resize" }
                    MItem("Putar 90° kanvas") { menu = false; e.rotate90() }
                    MItem("Balik horizontal (layer)") { menu = false; e.flipLayer(true) }
                    MItem("Balik vertikal (layer)") { menu = false; e.flipLayer(false) }
                    MItem("Bersihkan layer") { menu = false; e.clearLayer() }
                    MItem("Pas layar") { menu = false; e.fit() }
                    MItem("Bahasa") { menu = false; dlg = "lang" }
                    MItem("Tentang") { menu = false; dlg = "about" }
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PixelCanvas(e)
            Text(
                "${e.width}×${e.height}  •  ${(e.zoom * 100).roundToInt()}%  •  L${e.active + 1}/${e.layers.size}",
                color = Color(0xCCFFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color(0x66000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            if (e.floating != null) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { e.commitFloat() }) {
                        Icon(Lu.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tempel")
                    }
                    OutlinedButton(onClick = { e.cancelFloat() }) {
                        Icon(Lu.X, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Batal")
                    }
                }
            }
        }

        BottomPanel(e, onColor = { dlg = "color" }, onLayers = { dlg = "layers" }, onClips = { dlg = "clips" })
    }

    when (dlg) {
        "new" -> SizeDialog(
            "Kanvas baru", 32, 32,
            "Proyek saat ini akan diganti. Simpan dulu kalau perlu.",
            { w, h -> dlg = ""; e.newDoc(w, h) }, { dlg = "" }
        )
        "resize" -> SizeDialog(
            "Ubah ukuran kanvas", e.width, e.height,
            "Ukuran lebih kecil memotong gambar dari sisi kanan/bawah.",
            { w, h -> dlg = ""; e.resizeCanvas(w, h) }, { dlg = "" }
        )
        "lang" -> LanguageDialog { dlg = "" }
        "color" -> ColorDialog(e) { dlg = "" }
        "layers" -> LayersDialog(e) { dlg = "" }
        "clips" -> ClipsDialog(e) { dlg = "" }
        "export" -> ExportDialog(e, { sc -> dlg = ""; a.exportPng(sc) }, { dlg = "" })
        "about" -> AlertDialog(
            onDismissRequest = { dlg = "" },
            title = { Text("Pixilio : Pixel Art") },
            text = {
                Text(
                    tr("Versi") + " ${a.version}\n\n" +
                        tr(
                            "Editor pixel art dengan layer, clipboard (salin/potong/tempel), " +
                                "simetri, ekspor PNG berskala, dan proyek .pixilio. Ukuran kanvas tidak dibatasi " +
                                "aplikasi (batasnya RAM perangkat)."
                        )
                )
            },
            confirmButton = { TextButton(onClick = { dlg = "" }) { Text("OK") } }
        )
    }
}

@Composable
fun MItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick)
}

@Composable
fun PixelCanvas(e: Editor) {
    Canvas(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { e.onViewSize(it) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var multi = false
                    e.pointerDown(down.position)
                    do {
                        val ev = awaitPointerEvent()
                        val pressed = ev.changes.count { it.pressed }
                        if (pressed >= 2) {
                            if (!multi) {
                                multi = true
                                e.pointerCancel()
                            }
                            val z = ev.calculateZoom()
                            val pan = ev.calculatePan()
                            val c = ev.calculateCentroid()
                            if (c.isSpecified) e.transform(c, z, pan)
                            ev.changes.forEach { it.consume() }
                        } else if (!multi && pressed == 1) {
                            val ch = ev.changes.firstOrNull { it.pressed }
                            if (ch != null) {
                                e.pointerMove(ch.position)
                                ch.consume()
                            }
                        }
                    } while (ev.changes.any { it.pressed })
                    if (!multi) e.pointerUp()
                }
            }
    ) {
        val ver = e.version
        val z = e.zoom
        val o = e.offset
        drawRect(Color(0xFF3A3A3A))
        val bmp = e.bitmap ?: return@Canvas
        val w = e.width
        val h = e.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        drawChecker(o.x, o.y, w * z, h * z, CHECKER_PX * z)

        drawImage(
            image = bmp.asImageBitmap(),
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset(o.x.roundToInt(), o.y.roundToInt()),
            dstSize = IntSize((w * z).roundToInt().coerceAtLeast(1), (h * z).roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None
        )
        drawRect(Color(0xFF999999), topLeft = o, size = Size(w * z, h * z), style = Stroke(1f))

        if (e.showGrid && z >= 8f) {
            val gc = Color(0x66808080)
            val gx0 = max(0, floor(-o.x / z).toInt())
            val gx1 = min(w, ceil((size.width - o.x) / z).toInt())
            val gy0 = max(0, floor(-o.y / z).toInt())
            val gy1 = min(h, ceil((size.height - o.y) / z).toInt())
            for (gx in gx0..gx1) {
                val sx = o.x + gx * z
                drawLine(gc, Offset(sx, o.y + gy0 * z), Offset(sx, o.y + gy1 * z), 1f)
            }
            for (gy in gy0..gy1) {
                val sy = o.y + gy * z
                drawLine(gc, Offset(o.x + gx0 * z, sy), Offset(o.x + gx1 * z, sy), 1f)
            }
        }

        val n = e.pvCount
        if (n > 0) {
            val arr = e.pvArr
            val pc = Color(e.color)
            for (k in 0 until n) {
                val i = arr[k]
                val sx = o.x + (i % w) * z
                val sy = o.y + (i / w) * z
                if (sx + z < 0f || sy + z < 0f || sx > size.width || sy > size.height) continue
                drawRect(pc, Offset(sx, sy), Size(z, z))
            }
        }

        val s = if (e.tool == Tool.SELECT) e.selection else null
        if (s != null) {
            val tl = Offset(o.x + s[0] * z, o.y + s[1] * z)
            val sz = Size((s[2] - s[0] + 1) * z, (s[3] - s[1] + 1) * z)
            drawRect(Color.Black, tl, sz, style = Stroke(3f))
            drawRect(Color.White, tl, sz, style = Stroke(2f, pathEffect = dash))
        }

        val fl = e.floating
        if (fl != null) {
            val fp = e.floatPos
            val dx = o.x + floor(fp.x) * z
            val dy = o.y + floor(fp.y) * z
            drawImage(
                image = fl.bitmap.asImageBitmap(),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(fl.w, fl.h),
                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                dstSize = IntSize((fl.w * z).roundToInt().coerceAtLeast(1), (fl.h * z).roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            drawRect(Color(0xFFFFEB3B), Offset(dx, dy), Size(fl.w * z, fl.h * z), style = Stroke(2f, pathEffect = dash))
        }
    }
}

@Composable
fun BottomPanel(e: Editor, onColor: () -> Unit, onLayers: () -> Unit, onClips: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Tool.values().forEach { t ->
                FilterChip(
                    selected = e.tool == t,
                    onClick = { e.tool = t },
                    label = { Text(t.label) },
                    leadingIcon = chipIcon(toolIcon(t))
                )
            }
        }

        if (e.tool == Tool.SELECT) SelectionBar(e)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF777777))
                    .padding(2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(e.color))
                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
                    .clickable { onColor() }
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                e.recent.toList().forEach { c -> Swatch(c, e.color == c, { e.useColor(c) }, null) }
                if (e.recent.isNotEmpty()) Spacer(Modifier.width(6.dp))
                e.palette.toList().forEach { c -> Swatch(c, e.color == c, { e.useColor(c) }, { e.removePalette(c) }) }
            }
        }

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(onClick = onLayers, label = { Text("Layer") }, leadingIcon = chipIcon(Lu.Layers))
            AssistChip(onClick = onClips, label = { Text("Clipboard (${e.clips.size})") }, leadingIcon = chipIcon(Lu.Clipboard))
            FilterChip(selected = e.showGrid, onClick = { e.showGrid = !e.showGrid }, label = { Text("Grid") }, leadingIcon = chipIcon(Lu.Grid3x3))
            FilterChip(selected = e.mirrorX, onClick = { e.mirrorX = !e.mirrorX }, label = { Text("Simetri X") }, leadingIcon = chipIcon(Lu.FlipHorizontal2))
            FilterChip(selected = e.mirrorY, onClick = { e.mirrorY = !e.mirrorY }, label = { Text("Simetri Y") }, leadingIcon = chipIcon(Lu.FlipVertical2))
            FilterChip(selected = e.shapeFill, onClick = { e.shapeFill = !e.shapeFill }, label = { Text("Isi bentuk") }, leadingIcon = chipIcon(Lu.Square))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ukuran ${e.brush}", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(78.dp))
            Slider(
                value = e.brush.toFloat(),
                onValueChange = { e.brush = it.roundToInt().coerceAtLeast(1) },
                valueRange = 1f..64f,
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
fun SelectionBar(e: Editor) {
    val has = e.selection != null
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AssistChip(onClick = { e.selectAll() }, label = { Text("Semua") }, leadingIcon = chipIcon(Lu.BoxSelect))
        AssistChip(onClick = { e.copySelection(false) }, enabled = has, label = { Text("Salin") }, leadingIcon = chipIcon(Lu.Copy))
        AssistChip(onClick = { e.copySelection(true) }, enabled = has, label = { Text("Potong") }, leadingIcon = chipIcon(Lu.Scissors))
        AssistChip(onClick = { e.copySelection(false, true) }, enabled = has, label = { Text("Salin gabungan") }, leadingIcon = chipIcon(Lu.Layers))
        AssistChip(onClick = { e.deleteSelection() }, enabled = has, label = { Text("Hapus") }, leadingIcon = chipIcon(Lu.Trash2))
        AssistChip(
            onClick = {
                val c = e.clips.firstOrNull()
                if (c != null) e.paste(c) else e.onMessage("Clipboard masih kosong")
            },
            label = { Text("Tempel") },
            leadingIcon = chipIcon(Lu.Clipboard)
        )
        AssistChip(onClick = { e.clearSelection() }, enabled = has, label = { Text("Batal pilih") }, leadingIcon = chipIcon(Lu.X))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Swatch(c: Int, selected: Boolean, onClick: () -> Unit, onLong: (() -> Unit)?) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(30.dp)
            .clip(shape)
            .background(Color(c))
            .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color(0x66FFFFFF), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
    )
}

@Composable
fun SizeDialog(
    title: String,
    w0: Int,
    h0: Int,
    note: String,
    onOk: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var ws by remember { mutableStateOf(w0.toString()) }
    var hs by remember { mutableStateOf(h0.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(note, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(16, 32, 64, 128).forEach { s ->
                        AssistChip(onClick = { ws = s.toString(); hs = s.toString() }, label = { Text("$s") })
                    }
                }
                OutlinedTextField(
                    value = ws,
                    onValueChange = { ws = it.filter(Char::isDigit).take(7) },
                    label = { Text("Lebar (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = hs,
                    onValueChange = { hs = it.filter(Char::isDigit).take(7) },
                    label = { Text("Tinggi (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = ws.toIntOrNull()
                val h = hs.toIntOrNull()
                if (w != null && h != null && w > 0 && h > 0) onOk(w, h)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun LayersDialog(e: Editor, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Layer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(e.layers.size) { k ->
                        val i = e.layers.size - 1 - k
                        val l = e.layers.getOrNull(i) ?: return@items
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i == e.active) Color(0x3329B6F6) else Color.Transparent)
                                .clickable { e.active = i }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { e.setVisible(l, !l.visible) }) {
                                Icon(if (l.visible) Lu.Eye else Lu.EyeOff, null, Modifier.size(20.dp))
                            }
                            Text(l.name, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(l.opacity * 100).roundToInt()}%", fontSize = 12.sp)
                        }
                    }
                }
                val al = e.layers.getOrNull(e.active)
                if (al != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opasitas", fontSize = 13.sp)
                        Slider(
                            value = al.opacity,
                            onValueChange = { e.setOpacity(al, it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        )
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = { e.addLayer() }) { Text("+ Baru") }
                    TextButton(onClick = { e.duplicateLayer() }) { Text("Duplikat") }
                    TextButton(onClick = { e.deleteLayer() }) { Text("Hapus") }
                    TextButton(onClick = { e.moveLayer(1) }) { Text("↑") }
                    TextButton(onClick = { e.moveLayer(-1) }) { Text("↓") }
                    TextButton(onClick = { e.mergeDown() }) { Text("Gabung ↓") }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Tutup") }
            }
        }
    }
}

@Composable
fun ClipsDialog(e: Editor, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Clipboard (${e.clips.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (e.clips.isEmpty()) {
                    Text(
                        "Kosong. Pakai alat Pilih, blok area, lalu Salin atau Potong.",
                        fontSize = 13.sp
                    )
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(e.clips.size) { k ->
                        val c = e.clips.getOrNull(k) ?: return@items
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.background(Color(0xFF888888))) {
                                Image(
                                    bitmap = c.bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    contentScale = ContentScale.Fit,
                                    filterQuality = FilterQuality.None
                                )
                            }
                            Text(c.label, modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp), fontSize = 13.sp)
                            TextButton(onClick = { e.paste(c); onDismiss() }) { Text("Tempel") }
                            IconButton(onClick = { e.clips.remove(c) }) { Icon(Lu.X, null, Modifier.size(18.dp)) }
                        }
                    }
                }
                Row(Modifier.align(Alignment.End)) {
                    if (e.clips.isNotEmpty()) TextButton(onClick = { e.clips.clear() }) { Text("Kosongkan") }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(e: Editor, onOk: (Int) -> Unit, onDismiss: () -> Unit) {
    var s by remember { mutableStateOf("1") }
    val sc = s.toIntOrNull() ?: 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekspor PNG") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 4, 8, 16).forEach { v ->
                        AssistChip(onClick = { s = v.toString() }, label = { Text("${v}x") })
                    }
                }
                OutlinedTextField(
                    value = s,
                    onValueChange = { s = it.filter(Char::isDigit).take(3) },
                    label = { Text("Skala (x)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Hasil: ${e.width.toLong() * sc} × ${e.height.toLong() * sc} px", fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = { if (sc >= 1) onOk(sc) }) { Text("Ekspor") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
