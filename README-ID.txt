RIALZ TKA AI - PROJECT ANDROID

Project ini adalah kerangka aplikasi Android native Kotlin.
Fitur:
- Floating bubble di atas aplikasi lain setelah izin overlay diberikan.
- Menu mengambang dengan shortcut ke fitur belajar di aplikasi.
- OpenRouter API untuk tutor/penjelasan.
- Pilih foto soal dari galeri untuk dianalisis.
- API key disimpan lokal di SharedPreferences.

PENTING:
- Aplikasi ini untuk latihan dan pembelajaran, bukan mengisi/mengirim jawaban ujian aktif.
- HTML tidak dapat melakukan overlay lintas aplikasi; proyek ini native Android.
- Jangan memasukkan API key pribadi ke repositori publik. Masukkan langsung di aplikasi.
- Build APK melalui GitHub Actions: unggah folder project ke repository GitHub, buka tab Actions, jalankan workflow "Build Rialz TKA AI APK", lalu unduh artifact.
- Untuk distribusi publik, pindahkan panggilan API ke backend agar API key tidak dapat diekstrak dari perangkat.
