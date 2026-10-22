# Changelog

## 0.0.20.2026-alpha
- Beranda: tampilan proyek bisa grid atau memanjang (list), pilihan diingat
- Beranda: pencarian proyek/folder, tidak peduli huruf besar atau kecil
- Halaman Pengaturan baru (bahasa, lisensi, tentang) lewat ikon di Beranda; Lisensi memuat lisensi pustaka yang dipakai
- Latar kotak-kotak kanvas: yang tadinya putih jadi abu-abu agak gelap
- Menu titik tiga proyek: tambah Ekspor file .pixilio dan Ekspor PNG
## 0.0.19.2026-alpha
- Beranda: tombol + sekarang membuka pilihan Proyek baru / Impor gambar; gambar diimpor jadi proyek baru di folder yang sedang dibuka

## 0.0.18.2026-alpha
- README ditulis ulang khusus untuk pengguna (cara download dan fitur), info build dan signing dihapus

## 0.0.17.2026-alpha
- Ikon aplikasi jadi adaptive icon: ikut bentuk bulat/squircle launcher (Android 8+), Android 7 tetap ikon kotak

## 0.0.16.2026-alpha
- Ikon aplikasi: hati merah pixel art baru (12×12)

## 0.0.15.2026-alpha
- Peringatan input dilengkapi: kode warna tidak valid, nama sudah dipakai (folder/ganti nama), karakter nama yang diganti spasi
- Ekspor PNG: nomor urut hari itu hanya naik kalau ekspor berhasil; Android 9 ke bawah tidak menimpa file yang sudah ada

## 0.0.14.2026-alpha
- Ubah ukuran kanvas: toggle "Skala gambar" (default mati) untuk men-skala isi gambar, bukan memotong/menambah ruang
- Pesan peringatan untuk input yang tidak sesuai (ukuran kosong/0/terlalu besar, memori, skala, nama kosong, rasio berubah, dll.)
- Ekspor PNG langsung ke `Pictures/Pixilio/<dd.MM.yyyy HH.mm N>.png` (N = urutan gambar di hari itu); izin penyimpanan hanya diminta di Android 9 ke bawah

## 0.0.13.2026-alpha
- Latar kotak-kotak: 1 kotak = 16 piksel kanvas (kanvas 16 polos, 32 = 2x2, 64 = 4x4, 128 = 8x8), berlaku juga di preview proyek
- Pengaturan ukuran latar dihapus

## 0.0.12.2026-alpha
- Fix: latar kotak-kotak benar-benar statis, terkunci di layar dan tidak ikut geser/zoom kanvas

## 0.0.11.2026-alpha
- Pixilio muncul di "Buka dengan" dan Bagikan: gambar (PNG/JPG/WebP) diimpor jadi proyek baru, file `.pixilio` langsung dibuka
- Support semua file manager (ZArchiver, Explorer, bawaan, dll.)

## 0.0.10.2026-alpha
- Latar kotak-kotak statis (tidak ikut zoom/ukuran piksel), ukuran bisa dipilih 16/32/64/128/256 px (Beranda dan menu ⋮ editor)
- Beranda: preview gambar tiap proyek
- Memori: buang buffer komposit penuh, simpan tanpa salinan layer, kanvas besar (mis. 2048) lebih hemat RAM

## 0.0.9.2026-alpha
- Warna: blend layer/opasitas dibulatkan (bukan dipotong), hasil lebih akurat
- Kode warna: dukung persen (`rgb(100%,0%,0%)`, alpha `50%`), hue HSL tidak lagi tampil 360
- Impor gambar: dipaksa ARGB_8888 tanpa scaling

## 0.0.8
- Warna: slider HSL tidak melayang; pipet menolak piksel transparan; ekspor PNG & impor gambar tanpa pembulatan alpha
- Bahasa: menu Default / Indonesia / English di Beranda dan menu ⋮ editor
- Ikon: seluruh ikon memakai gaya Lucide (path tertanam)

## 0.0.7
- Fix: sisa tampilan alat Pilih tidak lagi tertinggal saat pindah ke alat lain
