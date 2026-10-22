package com.pixilio.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** Validasi input: pesan error (menghalangi) dan peringatan (tidak menghalangi). */
object Check {
    fun dim(s: String, minMsg: String = "Minimal 1 px"): String? = when {
        s.isEmpty() -> "Wajib diisi"
        s.toIntOrNull() == null -> "Angka tidak valid"
        s.toInt() < 1 -> minMsg
        else -> null
    }

    private val badName = Regex("[\\\\/:*?\"<>|\\n\\r\\t]")

    /** Peringatan kalau nama mengandung karakter yang akan diganti spasi. */
    fun nameChars(s: String): String? =
        if (badName.containsMatchIn(s)) "Karakter \\ / : * ? \" < > | akan diganti spasi" else null

    fun tooBig(w: Long, h: Long): String? =
        if (w * h > Int.MAX_VALUE.toLong()) "Ukuran terlalu besar" else null

    fun memory(w: Long, h: Long, layers: Int): String? {
        val need = w * h * 4L * (layers + 2)
        return if (need > Runtime.getRuntime().maxMemory() * 7 / 10) "Ukuran ini kemungkinan melebihi memori perangkat" else null
    }
}

@Composable
fun Warn(text: String) {
    Text(text, color = Color(0xFFFFB74D), fontSize = 12.sp)
}
