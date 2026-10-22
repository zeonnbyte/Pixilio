# Changelog

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
