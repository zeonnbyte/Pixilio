package com.pixilio.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    root: File,
    dir: File,
    refresh: Int,
    grid: Boolean,
    onGrid: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onDir: (File) -> Unit,
    onOpen: (File) -> Unit,
    onCreate: (String, Int, Int) -> Unit,
    onImportImage: () -> Unit,
    onFolder: (String) -> Boolean,
    onRename: (Entry, String) -> Boolean,
    onDelete: (Entry) -> Unit,
    onExportFile: (Entry) -> Unit,
    onExportPng: (Entry, Int) -> Unit
) {
    var tick by remember { mutableIntStateOf(0) }
    var dlg by remember { mutableStateOf("") }
    var plusMenu by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<Entry?>(null) }
    var searching by remember(dir) { mutableStateOf(false) }
    var query by remember(dir) { mutableStateOf("") }
    val all = remember(dir, refresh, tick) { ProjectStore.list(dir) }
    val q = query.trim()
    // pencarian tidak peduli huruf besar/kecil
    val entries = if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) }
    val atRoot = dir == root
    val path = if (atRoot) "" else dir.relativeTo(root).path.replace(File.separatorChar, '/')

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!atRoot) {
                    IconButton(onClick = { dir.parentFile?.let(onDir) }) { Icon(Lu.ChevronLeft, null, tint = Color.White) }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (atRoot) "Pixilio" else dir.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (atRoot) "Proyek kamu" else "Pixilio / " + path.replace("/", " / "),
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {
                    if (searching) {
                        searching = false
                        query = ""
                    } else {
                        searching = true
                    }
                }) { Icon(if (searching) Lu.X else Lu.Search, null, tint = Color.White) }
                IconButton(onClick = { onGrid(!grid) }) {
                    Icon(if (grid) Lu.ListLines else Lu.LayoutGrid, null, tint = Color.White)
                }
                IconButton(onClick = onSettings) { Icon(Lu.Sliders, null, tint = Color.White) }
            }
            if (searching) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    placeholder = { Text("Cari proyek") },
                    leadingIcon = { Icon(Lu.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) ({
                        IconButton(onClick = { query = "" }) { Icon(Lu.X, null) }
                    }) else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .focusRequester(focus)
                )
            }
            if (all.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada isi.\nKetuk + untuk proyek baru, atau ikon folder untuk folder.",
                        color = Color(0x99FFFFFF),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "Tidak ada hasil untuk \"$q\"",
                        color = Color(0x99FFFFFF),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.file.path }) { e ->
                        EntryCard(
                            e,
                            onClick = { if (e.isDir) onDir(e.file) else onOpen(e.file) },
                            onRename = { target = e; dlg = "rename" },
                            onExportFile = { onExportFile(e) },
                            onExportPng = { target = e; dlg = "exportpng" },
                            onDelete = { target = e; dlg = "delete" }
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.file.path }) { e ->
                        EntryRow(
                            e,
                            onClick = { if (e.isDir) onDir(e.file) else onOpen(e.file) },
                            onRename = { target = e; dlg = "rename" },
                            onExportFile = { onExportFile(e) },
                            onExportPng = { target = e; dlg = "exportpng" },
                            onDelete = { target = e; dlg = "delete" }
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(onClick = { dlg = "folder" }) { Icon(Lu.FolderPlus, null) }
            Box {
                FloatingActionButton(onClick = { plusMenu = true }) { Icon(Lu.Plus, null) }
                DropdownMenu(expanded = plusMenu, onDismissRequest = { plusMenu = false }) {
                    DropdownMenuItem(text = { Text("Proyek baru") }, onClick = { plusMenu = false; dlg = "project" })
                    DropdownMenuItem(text = { Text("Impor gambar") }, onClick = { plusMenu = false; onImportImage() })
                }
            }
        }
    }

    val t = target
    when (dlg) {
        "project" -> ProjectDialog({ n, w, h -> dlg = ""; onCreate(n, w, h) }, { dlg = "" })
        "folder" -> NameDialog("Folder baru", "", "Nama folder", { n -> all.any { it.isDir && it.name == n } }, { n ->
            dlg = ""
            if (onFolder(n)) tick++
        }, { dlg = "" })
        "rename" -> if (t != null) NameDialog("Ganti nama", t.name, "Nama", { n -> all.any { it !== t && it.isDir == t.isDir && it.name == n } }, { n ->
            dlg = ""
            if (onRename(t, n)) tick++
        }, { dlg = "" })
        "exportpng" -> if (t != null) {
            val sz = remember(t.file.path) { ProjectIO.readSize(t.file) }
            if (sz == null) {
                AlertDialog(
                    onDismissRequest = { dlg = "" },
                    title = { Text("Ekspor PNG") },
                    text = { Text("File proyek rusak") },
                    confirmButton = { TextButton(onClick = { dlg = "" }) { Text("OK") } }
                )
            } else {
                ExportDialog(sz.first, sz.second, { sc -> dlg = ""; onExportPng(t, sc) }, { dlg = "" })
            }
        }
        "delete" -> if (t != null) AlertDialog(
            onDismissRequest = { dlg = "" },
            title = { Text(if (t.isDir) "Hapus folder?" else "Hapus proyek?") },
            text = {
                Text("\"${t.name}\" akan dihapus permanen" + if (t.isDir) " beserta isinya." else ".")
            },
            confirmButton = {
                TextButton(onClick = {
                    dlg = ""
                    onDelete(t)
                    tick++
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { dlg = "" }) { Text("Batal") } }
        )
    }
}

private fun entryInfo(e: Entry): String =
    if (e.isDir) {
        "${ProjectStore.count(e.file)} item"
    } else {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(e.file.lastModified())) +
            "  •  " + (e.file.length() / 1024) + " KB"
    }

/** Menu titik tiga: ganti nama, ekspor (khusus proyek), hapus. */
@Composable
private fun EntryMenu(
    e: Entry,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onExportFile: () -> Unit,
    onExportPng: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Ganti nama") }, onClick = { onDismiss(); onRename() })
        if (!e.isDir) {
            DropdownMenuItem(text = { Text("Ekspor file .pixilio") }, onClick = { onDismiss(); onExportFile() })
            DropdownMenuItem(text = { Text("Ekspor PNG") }, onClick = { onDismiss(); onExportPng() })
        }
        DropdownMenuItem(text = { Text("Hapus") }, onClick = { onDismiss(); onDelete() })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    e: Entry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onExportFile: () -> Unit,
    onExportPng: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (e.isDir) {
                Icon(Lu.Folder, null, tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Thumb(e.file, Modifier.size(56.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entryInfo(e), color = Color(0x99FFFFFF), fontSize = 12.sp, maxLines = 1)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Lu.EllipsisVertical, null, tint = Color.White) }
                EntryMenu(e, menu, { menu = false }, onRename, onExportFile, onExportPng, onDelete)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryCard(
    e: Entry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onExportFile: () -> Unit,
    onExportPng: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.combinedClickable(onClick = onClick, onLongClick = { menu = true })) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (e.isDir) {
                    Icon(Lu.Folder, null, tint = Color.White, modifier = Modifier.size(64.dp))
                } else {
                    Thumb(e.file, Modifier.fillMaxSize())
                }
            }
            Row(
                Modifier.padding(start = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(entryInfo(e), color = Color(0x99FFFFFF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Lu.EllipsisVertical, null, tint = Color.White) }
                    EntryMenu(e, menu, { menu = false }, onRename, onExportFile, onExportPng, onDelete)
                }
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    label: String,
    taken: (String) -> Boolean = { false },
    onOk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var s by remember { mutableStateOf(initial) }
    val err = when {
        ProjectStore.clean(s).isEmpty() -> "Nama tidak boleh kosong"
        taken(ProjectStore.clean(s)) -> "Nama sudah dipakai"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = s,
                    onValueChange = { s = it.take(60) },
                    label = { Text(label) },
                    isError = err != null,
                    supportingText = if (err != null) ({ Text(err) }) else null,
                    singleLine = true
                )
                Check.nameChars(s)?.let { Warn(it) }
            }
        },
        confirmButton = { TextButton(enabled = err == null, onClick = { onOk(s) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ProjectDialog(onOk: (String, Int, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(tr("Proyek baru")) }
    var ws by remember { mutableStateOf("32") }
    var hs by remember { mutableStateOf("32") }
    val errN = if (name.isBlank()) "Nama tidak boleh kosong" else null
    val errW = Check.dim(ws)
    val errH = Check.dim(hs)
    val w = ws.toIntOrNull() ?: 0
    val h = hs.toIntOrNull() ?: 0
    val valid = errW == null && errH == null
    val big = if (valid) Check.tooBig(w.toLong(), h.toLong()) else null
    val ok = errN == null && valid && big == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proyek baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Nama proyek") },
                    isError = errN != null,
                    supportingText = if (errN != null) ({ Text(errN) }) else null,
                    singleLine = true
                )
                Check.nameChars(name)?.let { Warn(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(16, 32, 64, 128).forEach { v ->
                        AssistChip(onClick = { ws = v.toString(); hs = v.toString() }, label = { Text("$v") })
                    }
                }
                OutlinedTextField(
                    value = ws,
                    onValueChange = { ws = it.filter(Char::isDigit).take(7) },
                    label = { Text("Lebar (px)") },
                    isError = errW != null,
                    supportingText = if (errW != null) ({ Text(errW) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = hs,
                    onValueChange = { hs = it.filter(Char::isDigit).take(7) },
                    label = { Text("Tinggi (px)") },
                    isError = errH != null,
                    supportingText = if (errH != null) ({ Text(errH) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (big != null) Text(big, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (ok) Check.memory(w.toLong(), h.toLong(), 1)?.let { Warn(it) }
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onOk(name, w, h) }) { Text("Buat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun Thumb(f: File, modifier: Modifier) {
    val img by produceState<ImageBitmap?>(null, f.path, f.lastModified()) {
        value = withContext(Dispatchers.IO) { ProjectIO.loadThumb(f)?.asImageBitmap() }
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        val b = img
        if (b == null) {
            Icon(Lu.Palette, null, tint = Color.White, modifier = Modifier.size(28.dp))
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val iw = b.width
                val ih = b.height
                val sc = minOf(size.width / iw, size.height / ih)
                val dw = iw * sc
                val dh = ih * sc
                val l = (size.width - dw) / 2f
                val t = (size.height - dh) / 2f
                drawChecker(l, t, dw, dh, CHECKER_PX * sc)
                drawImage(
                    image = b,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(iw, ih),
                    dstOffset = IntOffset(l.roundToInt(), t.roundToInt()),
                    dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
                    filterQuality = FilterQuality.None
                )
            }
        }
    }
}
