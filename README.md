<div align="center">

# Pixilio

**Editor pixel art untuk Android**

![Versi](https://img.shields.io/badge/versi-0.0.15--alpha-orange)
![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)

</div>

Pixilio adalah editor pixel art ringan buat Android, dibuat dengan Kotlin dan Jetpack Compose. Tanpa iklan, tanpa batas ukuran kanvas dari aplikasi (batasnya RAM perangkat).

## Fitur

- **Beranda:** daftar proyek dengan preview gambar, folder, tombol **+** proyek baru, ganti nama, hapus, autosave
- **Alat:** pensil, penghapus, isi (fill), pipet, garis, kotak, elips, pilih area, geser
- **Kuas:** ukuran 1-64, simetri X/Y, isi bentuk
- **Warna:** palet 32 warna, color picker mode **Wheel** dan **Biasa**, kode HEX / RGB / HSL / HSV / CMYK / 0xARGB, palet tersimpan
- **Layer:** tambah, duplikat, hapus, urutan, gabung ke bawah, visibilitas, opasitas
- **Clipboard:** salin, potong, tempel, riwayat clipboard
- **Kanvas:** undo/redo, pinch zoom, grid, ubah ukuran (opsi skala gambar), putar 90°, balik horizontal/vertikal
- **File:** proyek `.pixilio`, ekspor PNG berskala langsung ke `Pictures/Pixilio`, impor gambar
- **Buka dengan:** Pixilio muncul di daftar "Buka dengan" / Bagikan untuk gambar dan file `.pixilio` dari file manager mana pun (ZArchiver, Explorer, bawaan, dll.)
- **Latar:** kotak-kotak, 1 kotak = 16 piksel kanvas (kanvas 16 = polos, 32 = 2×2, 64 = 4×4, dst)
- **Validasi:** pesan peringatan untuk input yang tidak sesuai (ukuran, skala, nama, memori)
- **Bahasa:** Default (ikut sistem), Indonesia, English
- **Ikon:** gaya [Lucide](https://lucide.dev)

## Instalasi

Download APK dari tab **Releases** atau dari artifact **Actions**, lalu install di HP (izinkan install dari sumber tidak dikenal).

## Build

**Android Studio:** buka folder ini (JDK 17), tunggu Gradle sync, lalu *Build > Build APK(s)*.

**GitHub Actions:** push ke `main` (atau jalankan manual di tab **Actions > Build APK**), lalu download artifact **Pixilio APK**.

**AndroidIDE:** buka folder project, sync Gradle, lalu *Build > Build APK*.

### Signing

APK release ditandatangani lewat GitHub Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Kalau kosong, build memakai debug key. Jangan commit file `.jks` dan simpan backup keystore-nya, kalau hilang update tidak bisa menimpa versi lama.

## Changelog

Lihat [CHANGELOG.md](CHANGELOG.md).
