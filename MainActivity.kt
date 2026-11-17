package com.rialz.tka

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("rialz", MODE_PRIVATE) }
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var key: EditText
    private lateinit var model: EditText
    private lateinit var question: EditText
    private lateinit var answer: TextView
    private var imageData: String? = null
    private var pendingMode = "ask"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingMode = intent.getStringExtra("mode") ?: "ask"
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = 1; setPadding(28, 24, 28, 24); setBackgroundColor(0xFFF6F3FA.toInt()) }
        val title = TextView(this).apply { text = "RIALZ TKA AI"; textSize = 24f; setTextColor(0xFF5520B8.toInt()); setTypeface(null, 1) }
        root.addView(title)
        root.addView(TextView(this).apply { text = "Smart Learning Assistant • OpenRouter"; textSize = 13f; setTextColor(0xFF777387.toInt()) })
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = 1; setPadding(0, 18, 0, 10) }
        key = edit("OpenRouter API Key", prefs.getString("key", "") ?: "", true)
        model = edit("Model", prefs.getString("model", "openai/gpt-4o-mini") ?: "", false)
        question = edit("Soal / pertanyaan latihan", "", false, true)
        body.addView(label("Pengaturan AI")); body.addView(key); body.addView(model)
        val save = button("SIMPAN API & MODEL") { prefs.edit().putString("key", key.text.toString().trim()).putString("model", model.text.toString().trim()).apply(); toast("Pengaturan disimpan di perangkat") }
        body.addView(save)
        body.addView(label("Asisten belajar"))
        body.addView(question)
        body.addView(button("PILIH FOTO SOAL") { pickImage() })
        body.addView(button("TANYA / ANALISIS DENGAN AI") { askAI(pendingMode) })
        body.addView(button("START FLOATING MENU") { startFloating() })
        body.addView(button("STOP FLOATING MENU") { stopService(Intent(this, FloatingService::class.java)); toast("Floating menu dihentikan") })
        answer = TextView(this).apply { text = "Hasil pembahasan akan muncul di sini."; textSize = 14f; setTextColor(0xFF29243A.toInt()); setPadding(18,18,18,18); setBackgroundColor(0xFFFFFFFF.toInt()) }
        body.addView(label("Hasil AI")); body.addView(answer)
        body.addView(TextView(this).apply { text = "Catatan: gunakan untuk latihan dan pembelajaran. Aplikasi tidak mengisi atau mengirim jawaban ke situs ujian. API key disimpan lokal pada perangkat; jangan bagikan kepada orang lain."; textSize=11f; setTextColor(0xFF777387.toInt()); setPadding(0,18,0,18) })
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
        if (pendingMode == "scan") pickImage()
    }

    private fun label(t: String) = TextView(this).apply { text=t; textSize=16f; setTextColor(0xFF29243A.toInt()); setTypeface(null,1); setPadding(0,18,0,8) }
    private fun edit(hintText: String, value: String, password: Boolean, multi: Boolean=false): EditText {
        return EditText(this).apply {
            hint=hintText; setText(value); textSize=14f; setPadding(16,12,16,12); setBackgroundColor(0xFFFFFFFF.toInt())
            if(password) inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            if(multi) { minLines=4; gravity=Gravity.TOP; }
        }
    }
    private fun button(t: String, action: () -> Unit) = Button(this).apply { text=t; setOnClickListener { action() } }
    private fun toast(t:String)=Toast.makeText(this,t,Toast.LENGTH_SHORT).show()

    private fun startFloating() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            toast("Izinkan tampil di atas aplikasi lain, lalu tekan START lagi")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 77)
        }
        startForegroundService(Intent(this, FloatingService::class.java))
        toast("Floating menu Rialz aktif")
    }
    private fun pickImage() {
        val i=Intent(Intent.ACTION_GET_CONTENT).apply { type="image/*" }
        startActivityForResult(i, 21)
    }
    @Deprecated("Legacy result callback for broad Android compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==21 && resultCode==RESULT_OK) {
            val uri=data?.data ?: return
            try {
                val bytes=contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
                imageData=android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                question.setText("Baca soal pada gambar ini untuk latihan. Jelaskan langkah penyelesaiannya.")
                toast("Foto soal siap dianalisis")
            } catch(e:Exception) { toast("Gagal membaca gambar: ${e.message}") }
        }
    }
    private fun askAI(mode:String) {
        val apiKey=key.text.toString().trim().ifBlank { prefs.getString("key","") ?: "" }
        val modelName=model.text.toString().trim().ifBlank { prefs.getString("model","openai/gpt-4o-mini") ?: "openai/gpt-4o-mini" }
        val q=question.text.toString().trim()
        if(apiKey.isBlank()){toast("Masukkan OpenRouter API Key dulu");return}
        if(q.isBlank() && imageData==null){toast("Tulis soal atau pilih foto dulu");return}
        answer.text="Rialz AI sedang menganalisis…"
        worker.execute {
            try {
                val modePrompt=when(mode) {
                    "scan" -> "Baca soal dari gambar untuk latihan, jelaskan konsep dan langkah penyelesaian. Jika teks tidak jelas, katakan."
                    "tutor" -> "Jadilah tutor yang sabar. Ajarkan konsep dan langkah penyelesaian soal latihan."
                    "check" -> "Periksa jawaban latihan siswa, jelaskan yang benar dan yang perlu diperbaiki."
                    "explain" -> "Berikan penjelasan lengkap dan runtut untuk soal latihan."
                    "summary" -> "Buat ringkasan materi terstruktur dengan konsep dan contoh."
                    else -> "Jawab pertanyaan belajar dengan jelas dan akurat."
                }
                val userContent: Any = if(imageData!=null) JSONArray()
                    .put(JSONObject().put("type","text").put("text", "$modePrompt\n\n${q}"))
                    .put(JSONObject().put("type","image_url").put("image_url", JSONObject().put("url","data:image/jpeg;base64,$imageData")))
                else "$modePrompt\n\n$q"
                val messages=JSONArray()
                    .put(JSONObject().put("role","system").put("content","Kamu Rialz TKA AI, tutor belajar yang teliti. Fokus pada latihan dan pemahaman. Jangan mengerjakan ujian aktif atau mengirim jawaban ke sistem ujian."))
                    .put(JSONObject().put("role","user").put("content",userContent))
                val payload=JSONObject().put("model",modelName).put("messages",messages).put("temperature",0.3)
                val conn=(URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection)
                conn.requestMethod="POST"; conn.connectTimeout=20000; conn.readTimeout=60000; conn.doOutput=true
                conn.setRequestProperty("Authorization","Bearer $apiKey"); conn.setRequestProperty("Content-Type","application/json")
                conn.setRequestProperty("X-Title","Rialz TKA AI")
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val stream=if(conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response=stream.bufferedReader().use{it.readText()}
                val json=JSONObject(response)
                if(conn.responseCode !in 200..299) throw Exception(json.optJSONObject("error")?.optString("message") ?: "HTTP ${conn.responseCode}")
                val result=json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
                runOnUiThread { answer.text=result }
                conn.disconnect()
            } catch(e:Exception) { runOnUiThread { answer.text="Gagal: ${e.message}\nPeriksa internet, API key, model, dan kredit OpenRouter." } }
        }
    }
}
