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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    root: File,
    dir: File,
    refresh: Int,
    onDir: (File) -> Unit,
    onOpen: (File) -> Unit,
    onCreate: (String, Int, Int) -> Unit,
    onFolder: (String) -> Boolean,
    onRename: (Entry, String) -> Boolean,
    onDelete: (Entry) -> Unit
) {
    var tick by remember { mutableIntStateOf(0) }
    var dlg by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<Entry?>(null) }
    val entries = remember(dir, refresh, tick) { ProjectStore.list(dir) }
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
                IconButton(onClick = { dlg = "lang" }) { Icon(Lu.Languages, null, tint = Color.White) }
            }
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada isi.\nKetuk + untuk proyek baru, atau ikon folder untuk folder.",
                        color = Color(0x99FFFFFF),
                        textAlign = TextAlign.Center
                    )
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
            FloatingActionButton(onClick = { dlg = "project" }) { Icon(Lu.Plus, null) }
        }
    }

    val t = target
    when (dlg) {
        "lang" -> LanguageDialog { dlg = "" }
        "project" -> ProjectDialog({ n, w, h -> dlg = ""; onCreate(n, w, h) }, { dlg = "" })
        "folder" -> NameDialog("Folder baru", "", "Nama folder", { n ->
            dlg = ""
            if (onFolder(n)) tick++
        }, { dlg = "" })
        "rename" -> if (t != null) NameDialog("Ganti nama", t.name, "Nama", { n ->
            dlg = ""
            if (onRename(t, n)) tick++
        }, { dlg = "" })
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(e: Entry, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val info = if (e.isDir) {
        "${ProjectStore.count(e.file)} item"
    } else {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(e.file.lastModified())) +
            "  •  " + (e.file.length() / 1024) + " KB"
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (e.isDir) Lu.Folder else Lu.Palette, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(info, color = Color(0x99FFFFFF), fontSize = 12.sp, maxLines = 1)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Lu.EllipsisVertical, null, tint = Color.White) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Ganti nama") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete() })
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
    onOk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var s by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = s, onValueChange = { s = it.take(60) }, label = { Text(label) }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { if (s.isNotBlank()) onOk(s) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ProjectDialog(onOk: (String, Int, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(tr("Proyek baru")) }
    var ws by remember { mutableStateOf("32") }
    var hs by remember { mutableStateOf("32") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proyek baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Nama proyek") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(16, 32, 64, 128).forEach { v ->
                        AssistChip(onClick = { ws = v.toString(); hs = v.toString() }, label = { Text("$v") })
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
                if (w != null && h != null && w > 0 && h > 0) onOk(name, w, h)
            }) { Text("Buat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
