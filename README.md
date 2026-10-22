# Pixilio : Pixel Art — V0.0.4.2026-alpha

Editor pixel art untuk Android (Kotlin + Jetpack Compose).

## Fitur
- Alat: pensil, penghapus, isi (flood fill), pipet, garis, kotak, elips, pilih area, geser
- Ukuran kuas 1–64, simetri X/Y, isi bentuk
- Clipboard: salin, potong, salin gabungan, tempel (bisa digeser dulu), riwayat clipboard
- Layer: tambah, duplikat, hapus, urutan, gabung ke bawah, visibilitas, opasitas
- Undo/redo, pinch zoom, geser dua jari, grid
- Palet 32 warna + warna kustom (RGBA/hex), palet tersimpan
- Ubah ukuran kanvas, putar 90°, balik horizontal/vertikal
- Proyek .pixilio (kompresi gzip), ekspor PNG berskala, impor gambar
- Autosave otomatis saat aplikasi ditutup
- Ukuran kanvas tidak dibatasi aplikasi (batas = RAM perangkat)

## Build
1. Buka folder ini di Android Studio (Jdk 17).
2. Tunggu Gradle sync, lalu Run atau Build > Build APK(s).

## Build di HP / tanpa laptop

### GitHub Actions (APK dibuild di cloud)
1. Buat repo GitHub, upload seluruh isi folder ini (termasuk `.github/workflows/build.yml`).
2. Buka tab **Actions** > **Build APK** > **Run workflow** (atau otomatis tiap push ke `main`).
3. Setelah selesai, download artifact **Pixilio APK** (isinya zip berisi `app-debug.apk`), extract, lalu install.

### AndroidIDE
Buka folder project di AndroidIDE, tunggu sync Gradle, lalu Build > Build APK.

## Tanda tangan APK (signing)
APK release ditandatangani dengan keystore yang disimpan sebagai GitHub Secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
Kalau secret kosong, build memakai debug key. Jangan pernah commit file `.jks`,
dan simpan backup keystore: kalau hilang, update app tidak bisa menimpa versi lama.
