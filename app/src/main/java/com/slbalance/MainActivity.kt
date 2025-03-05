package com.slbalance

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var text1: TextView
    private lateinit var text2: TextView
    private lateinit var edittext1: EditText
    private lateinit var button1: Button
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        text1 = findViewById(R.id.text1)
        text2 = findViewById(R.id.text2)
        edittext1 = findViewById(R.id.edittext1)
        button1 = findViewById(R.id.button1)

        val sharedPrefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val imsi = sharedPrefs.getString("imsi", null)
        val token = sharedPrefs.getString("token", null)

        if (imsi == null || token.isNullOrEmpty()) {
            text1.text = "Enter Order Link"
        } else {
            text1.visibility = TextView.GONE
            edittext1.setText("https://silent.link/order/$token")
            // Run API call asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                val balance = fetchBalance(imsi, token)
                runOnUiThread {
                    text2.text = "Balance: $$balance"
                }
            }
        }

        button1.setOnClickListener {
            val orderLink = edittext1.text.toString()
            val token = orderLink.substringAfterLast("/")

            CoroutineScope(Dispatchers.IO).launch {
                val imsi = fetchIMSI(token)
                if (imsi != null) {
                    sharedPrefs.edit().putString("imsi", imsi).putString("token", token).apply()
                    val balance = fetchBalance(imsi, token)
                    runOnUiThread {
                        text2.text = "Balance: $$balance"
                    }
                }
            }
        }
    }

    private fun fetchIMSI(token: String): String? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = """{"token": "$token"}""".trimIndent()
        val request = Request.Builder()
            .url("https://silent.link/api/v1/order")
            .post(json.toRequestBody(mediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseJson = gson.fromJson(response.body?.string(), Map::class.java)
            val data = responseJson["data"] as? Map<*, *>
            data?.get("imsi") as? String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchBalance(imsi: String, token: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = """{"phone": "$imsi", "token": "$token"}""".trimIndent()
        val request = Request.Builder()
            .url("https://silent.link/api/v1/checkbalance")
            .post(json.toRequestBody(mediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseJson = gson.fromJson(response.body?.string(), Map::class.java)
            val data = responseJson["data"] as? Map<*, *>
            String.format(Locale.US, "%.2f", data?.get("BALANCE") as? Double ?: 0.0)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error"
        }
    }
}