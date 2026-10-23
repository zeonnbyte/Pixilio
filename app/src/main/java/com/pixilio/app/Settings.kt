package com.pixilio.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class LicenseItem(val name: String, val kind: String, val text: String)

private val APP_LICENSE = LicenseItem(
    "Pixilio",
    "MIT License",
    "Copyright (c) 2026 Zeonn\n\n" +
        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and " +
        "associated documentation files (the \"Software\"), to deal in the Software without restriction, " +
        "including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, " +
        "and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, " +
        "subject to the following conditions:\n\n" +
        "The above copyright notice and this permission notice shall be included in all copies or substantial " +
        "portions of the Software.\n\n" +
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT " +
        "LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO " +
        "EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER " +
        "IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR " +
        "THE USE OR OTHER DEALINGS IN THE SOFTWARE."
)

private val LICENSES = listOf(
    LicenseItem(
        "Lucide Icons",
        "ISC License",
        "https://lucide.dev\n\n" +
            "Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT). " +
            "All other copyright (c) for Lucide are held by Lucide Contributors 2022.\n\n" +
            "Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee " +
            "is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.\n\n" +
            "THE SOFTWARE IS PROVIDED \"AS IS\" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE " +
            "INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE " +
            "FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS " +
            "OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING " +
            "OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE."
    ),
    LicenseItem(
        "AndroidX & Jetpack Compose",
        "Apache License 2.0",
        "https://developer.android.com/jetpack\n\n" +
            "Copyright (c) The Android Open Source Project\n\n" +
            "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this software except " +
            "in compliance with the License. You may obtain a copy of the License at\n\n" +
            "http://www.apache.org/licenses/LICENSE-2.0\n\n" +
            "Unless required by applicable law or agreed to in writing, software distributed under the License is " +
            "distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
            "See the License for the specific language governing permissions and limitations under the License."
    ),
    LicenseItem(
        "Kotlin",
        "Apache License 2.0",
        "https://kotlinlang.org\n\n" +
            "Copyright (c) JetBrains s.r.o. and Kotlin Programming Language contributors\n\n" +
            "Licensed under the Apache License, Version 2.0. You may obtain a copy of the License at\n\n" +
            "http://www.apache.org/licenses/LICENSE-2.0"
    )
)

@Composable
fun SettingsScreen(version: String, onBack: () -> Unit) {
    var page by remember { mutableStateOf("") }
    var dlg by remember { mutableStateOf("") }

    BackHandler(enabled = page != "") { page = "" }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .systemBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (page != "") page = "" else onBack() }) {
                Icon(Lu.ChevronLeft, null, tint = Color.White)
            }
            Text(
                if (page == "license") "Lisensi" else "Pengaturan",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
        if (page == "license") {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Lisensi aplikasi:", color = Color(0x99FFFFFF), fontSize = 13.sp)
                LicenseCard(APP_LICENSE)
                Text("Pixilio memakai pustaka open source berikut:", color = Color(0x99FFFFFF), fontSize = 13.sp)
                LICENSES.forEach { l -> LicenseCard(l) }
                Text("Pixilio $version", color = Color(0x66FFFFFF), fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val langLabel = when (Lang.mode) {
                    "id" -> "Indonesia"
                    "en" -> "English"
                    else -> "Default (ikut sistem)"
                }
                SettingRow("Bahasa", langLabel) { dlg = "lang" }
                SettingRow("Lisensi", "Lisensi Pixilio dan pustaka open source") { page = "license" }
                SettingRow("Tentang", "Versi $version") { dlg = "about" }
            }
        }
    }

    when (dlg) {
        "lang" -> LanguageDialog { dlg = "" }
        "about" -> AlertDialog(
            onDismissRequest = { dlg = "" },
            title = { Text("Pixilio : Pixel Art") },
            text = {
                Text(
                    tr("Versi") + " $version\n\n" +
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
private fun LicenseCard(l: LicenseItem) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(l.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(l.kind, color = Color(0xFFFFB74D), fontSize = 12.sp)
            Text(l.text, color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 17.sp)
                Text(subtitle, color = Color(0x99FFFFFF), fontSize = 12.sp)
            }
            Icon(Lu.ChevronRight, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(20.dp))
        }
    }
}
