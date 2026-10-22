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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Pengaturan bahasa: "system" (default, ikut bahasa sistem), "id", atau "en". */
object Lang {
    var mode by mutableStateOf("system")
        private set
    var onChange: (() -> Unit)? = null

    fun load(m: String) {
        mode = if (m == "id" || m == "en") m else "system"
    }

    fun set(m: String) {
        load(m)
        onChange?.invoke()
    }

    val resolved: String
        get() = when (mode) {
            "id" -> "id"
            "en" -> "en"
            else -> {
                val l = Locale.getDefault().language
                if (l == "in" || l == "id") "id" else "en"
            }
        }
}

private val EN: Map<String, String> = mapOf(
    "Proyek kamu" to "Your projects",
    "Belum ada isi.\nKetuk + untuk proyek baru, atau ikon folder untuk folder." to
        "Nothing here yet.\nTap + for a new project, or the folder icon for a folder.",
    "Hapus folder?" to "Delete folder?",
    "Hapus proyek?" to "Delete project?",
    "Hapus" to "Delete",
    "Ganti nama" to "Rename",
    "Batal" to "Cancel",
    "Proyek baru" to "New project",
    "Nama proyek" to "Project name",
    "Lebar (px)" to "Width (px)",
    "Tinggi (px)" to "Height (px)",
    "Buat" to "Create",
    "Folder baru" to "New folder",
    "Nama folder" to "Folder name",
    "Nama" to "Name",
    "Beranda" to "Home",
    "Kanvas baru (ganti isi)" to "New canvas (replace content)",
    "Impor file .pixilio (ganti isi)" to "Import .pixilio file (replace content)",
    "Ekspor file .pixilio" to "Export .pixilio file",
    "Ekspor PNG" to "Export PNG",
    "Impor gambar" to "Import image",
    "Ubah ukuran kanvas" to "Resize canvas",
    "Putar 90° kanvas" to "Rotate canvas 90°",
    "Balik horizontal (layer)" to "Flip horizontal (layer)",
    "Balik vertikal (layer)" to "Flip vertical (layer)",
    "Bersihkan layer" to "Clear layer",
    "Pas layar" to "Fit to screen",
    "Tentang" to "About",
    "Cari proyek" to "Search projects",
    "Pengaturan" to "Settings",
    "Lisensi" to "License",
    "Lisensi pustaka open source" to "Open source library licenses",
    "Pixilio memakai pustaka open source berikut:" to "Pixilio uses the following open source libraries:",
    "Proyek diekspor" to "Project exported",
    "Bahasa" to "Language",
    "Wajib diisi" to "Required",
    "Angka tidak valid" to "Invalid number",
    "Minimal 1 px" to "Minimum 1 px",
    "Minimal 1x" to "Minimum 1x",
    "Ukuran terlalu besar" to "Size too large",
    "Ukuran ini kemungkinan melebihi memori perangkat" to "This size may exceed device memory",
    "Ukuran sama dengan sekarang, tidak ada perubahan" to "Same as current size, nothing changes",
    "Ukuran lebih kecil: sebagian gambar akan terpotong" to "Smaller size: part of the image will be cropped",
    "Rasio berubah, gambar akan terlihat melar" to "Aspect ratio changes, image will look stretched",
    "Skala gambar (resize image)" to "Scale image (resize image)",
    "Gambar diskalakan ke ukuran baru." to "Image is scaled to the new size.",
    "Gambar tidak diskalakan: dipotong atau ditambah ruang kosong." to "Image is not scaled: cropped or padded with empty space.",
    "Nama tidak boleh kosong" to "Name cannot be empty",
    "Nama sudah dipakai" to "Name already in use",
    "Kode warna tidak valid" to "Invalid color code",
    "Karakter \\ / : * ? \" < > | akan diganti spasi" to "Characters \\ / : * ? \" < > | will be replaced with a space",
    "Izin penyimpanan ditolak, ekspor dibatalkan" to "Storage permission denied, export cancelled",
    "Disimpan ke Pictures/Pixilio" to "Saved to Pictures/Pixilio",
    "Tidak bisa membuat file" to "Cannot create file",
    "Tidak bisa membuat folder Pictures/Pixilio" to "Cannot create Pictures/Pixilio folder",
    "Default (ikut sistem)" to "Default (system)",
    "Kanvas baru" to "New canvas",
    "Proyek saat ini akan diganti. Simpan dulu kalau perlu." to
        "The current project will be replaced. Save first if needed.",
    "Ukuran lebih kecil memotong gambar dari sisi kanan/bawah." to
        "A smaller size crops the image from the right/bottom.",
    "Versi" to "Version",
    "Editor pixel art dengan layer, clipboard (salin/potong/tempel), simetri, ekspor PNG berskala, dan proyek .pixilio. Ukuran kanvas tidak dibatasi aplikasi (batasnya RAM perangkat)." to
        "Pixel art editor with layers, clipboard (copy/cut/paste), symmetry, scaled PNG export, and .pixilio projects. Canvas size is not limited by the app (the limit is device RAM).",
    "Tempel" to "Paste",
    "Simetri X" to "Mirror X",
    "Simetri Y" to "Mirror Y",
    "Isi bentuk" to "Fill shape",
    "Semua" to "All",
    "Salin" to "Copy",
    "Potong" to "Cut",
    "Salin gabungan" to "Copy merged",
    "Batal pilih" to "Deselect",
    "Clipboard masih kosong" to "Clipboard is empty",
    "Opasitas" to "Opacity",
    "+ Baru" to "+ New",
    "Duplikat" to "Duplicate",
    "Gabung ↓" to "Merge ↓",
    "Kosong. Pakai alat Pilih, blok area, lalu Salin atau Potong." to
        "Empty. Use the Select tool, mark an area, then Copy or Cut.",
    "Kosongkan" to "Clear",
    "Tutup" to "Close",
    "Skala (x)" to "Scale (x)",
    "Ekspor" to "Export",
    "Pilih warna" to "Pick color",
    "Biasa" to "Standard",
    "Kode warna" to "Color code",
    "+ Simpan ke palet & pakai" to "+ Save to palette & use",
    "Pakai" to "Use",
    "Pensil" to "Pencil",
    "Penghapus" to "Eraser",
    "Isi" to "Fill",
    "Pipet" to "Pipette",
    "Garis" to "Line",
    "Kotak" to "Rectangle",
    "Elips" to "Ellipse",
    "Pilih" to "Select",
    "Geser" to "Pan",
    "Nama folder kosong atau sudah dipakai" to "Folder name is empty or already used",
    "Nama kosong atau sudah dipakai" to "Name is empty or already used",
    "Proyek disimpan" to "Project saved",
    "Tidak bisa menulis file" to "Cannot write file",
    "Tidak bisa membaca file" to "Cannot read file",
    "Proyek dibuka" to "Project opened",
    "Bukan gambar yang valid" to "Not a valid image",
    "Gambar diimpor" to "Image imported",
    "Skala terlalu besar" to "Scale too large",
    "PNG tersimpan" to "PNG saved",
    "Format file tidak dikenali" to "Unrecognized file format",
    "File proyek rusak" to "Project file is corrupted",
    "Ukuran tidak valid" to "Invalid size",
    "Memori perangkat tidak cukup untuk ukuran ini" to "Not enough device memory for this size",
    "Memori tidak cukup untuk mengisi area sebesar ini" to "Not enough memory to fill an area this large",
    "Pilih area dulu dengan alat Pilih" to "Select an area first with the Select tool",
    "Dipotong ke clipboard" to "Cut to clipboard",
    "Disalin ke clipboard" to "Copied to clipboard",
    "Memori tidak cukup" to "Not enough memory",
    "Minimal harus ada satu layer" to "At least one layer is required",
    "Tidak ada layer di bawahnya" to "No layer below",
    "Piksel transparan, warna tidak diambil" to "Transparent pixel, color not picked"
)

private val RULES: List<Pair<Regex, (MatchResult) -> String>> = listOf(
    Regex("Gagal: (.*)", RegexOption.DOT_MATCHES_ALL) to { m: MatchResult -> "Failed: " + tr(m.groupValues[1]) },
    Regex("Tersimpan di (.*)", RegexOption.DOT_MATCHES_ALL) to { m: MatchResult -> "Saved to " + m.groupValues[1] },
    Regex("Tidak ada hasil untuk \"(.*)\"", RegexOption.DOT_MATCHES_ALL) to { m: MatchResult -> "No results for \"" + m.groupValues[1] + "\"" },
    Regex("Versi (.+)") to { m: MatchResult -> "Version " + m.groupValues[1] },
    Regex("(\\d+) item") to { m: MatchResult -> m.groupValues[1] + " items" },
    Regex("Ukuran (\\d+)") to { m: MatchResult -> "Size " + m.groupValues[1] },
    Regex("Hasil: (.*)") to { m: MatchResult -> "Result: " + m.groupValues[1] },
    Regex("(\\d+×\\d+) \\(gabungan\\)") to { m: MatchResult -> m.groupValues[1] + " (merged)" },
    Regex("\"(.*)\" akan dihapus permanen( beserta isinya)?\\.", RegexOption.DOT_MATCHES_ALL) to { m: MatchResult ->
        "\"" + m.groupValues[1] + "\" will be permanently deleted" +
            (if (m.groupValues[2].isNotEmpty()) " along with its contents" else "") + "."
    }
)

/** Terjemahkan teks (kunci = teks Indonesia). Kalau bahasa Indonesia atau tidak ada padanan, dikembalikan apa adanya. */
fun tr(s: String): String {
    if (Lang.resolved == "id") return s
    EN[s]?.let { return it }
    for ((r, f) in RULES) {
        val m = r.matchEntire(s)
        if (m != null) return f(m)
    }
    return s
}

/** Pengganti Text material3: otomatis menerjemahkan sesuai pengaturan bahasa. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    androidx.compose.material3.Text(
        text = tr(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val opts = listOf("system" to "Default (ikut sistem)", "id" to "Indonesia", "en" to "English")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bahasa") },
        text = {
            Column {
                opts.forEach { (k, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { Lang.set(k) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = Lang.mode == k, onClick = { Lang.set(k) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
