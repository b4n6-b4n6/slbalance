package com.slbalance

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var text1: TextView
    lateinit var text2: TextView
    lateinit var edittext1 : EditText
    lateinit var button1: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val gfgPolicy =
            ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(gfgPolicy)
        text1 = findViewById(R.id.text1)
        text2 = findViewById(R.id.text2)
        edittext1 = findViewById(R.id.edittext1)
        button1 = findViewById(R.id.button1)
        val sharedPrefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        if ((sharedPrefs.getString("imsi", null) == null) || (sharedPrefs.getString("token", null) == "")) {
            text1.text = "Enter Order Link"
        } else {
            text1.visibility = TextView.GONE
            val imsi = sharedPrefs.getString("imsi", null)
            val token = sharedPrefs.getString("token", null)
            edittext1.setText("https://silent.link/order/$token")
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = """
                {
                    "phone": "$imsi",
                    "token": "$token"
                }
            """.trimIndent()
            val request = okhttp3.Request.Builder()
                .url("https://silent.link/api/v1/checkbalance")
                .post(json.toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseJson = Gson().fromJson(response.body?.string(), Map::class.java)
            val data = responseJson["data"] as? Map<*, *>
            val balance = String.format(Locale.US, "%.2f", data?.get("BALANCE") as? Double)
            "Balance: $$balance".also { text2.text = it }
        }
        button1.setOnClickListener {
            val order_link = edittext1.text.toString()
            val token = order_link.substringAfterLast("/")
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json1 = """
                {
                    "token": "$token"
                }
            """.trimIndent()
            val req1 = okhttp3.Request.Builder()
                .url("https://silent.link/api/v1/order")
                .post(json1.toRequestBody(mediaType))
                .build()
            val resp1 = client.newCall(req1).execute()
            val resp1Json = Gson().fromJson(resp1.body?.string(), Map::class.java)
            val data1 = resp1Json["data"] as? Map<*, *>
            val imsi = data1?.get("imsi") as? String
            sharedPrefs.edit().putString("imsi", imsi).putString("token", token).apply()
            val json = """
                {
                    "phone": "$imsi",
                    "token": "$token"
                }
            """.trimIndent()
            val request = okhttp3.Request.Builder()
                .url("https://silent.link/api/v1/checkbalance")
                .post(json.toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseJson = Gson().fromJson(response.body?.string(), Map::class.java)
            val data = responseJson["data"] as? Map<*, *>
            val balance = String.format(Locale.US, "%.2f", data?.get("BALANCE") as? Double)
            "Balance: $$balance".also { text2.text = it }
        }
    }
}