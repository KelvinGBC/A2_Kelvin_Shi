package com.example.a2_kelvin_shi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import android.content.Intent
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var etBase: EditText
    private lateinit var etDest: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnConvert: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvRate: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnAbout: Button

    private val API_KEY = "fca_live_pfNc8LrSA99Kp8d1pbct7vFbRFmBynBPh1WoSMM4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBase = findViewById(R.id.etBase)
        etDest = findViewById(R.id.etDest)
        etAmount = findViewById(R.id.etAmount)
        btnConvert = findViewById(R.id.btnConvert)
        progress = findViewById(R.id.progress)
        tvRate = findViewById(R.id.tvRate)
        tvResult = findViewById(R.id.tvResult)
        btnAbout = findViewById(R.id.btnAbout)

        btnConvert.setOnClickListener { performConversion() }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun performConversion() {
        tvRate.text = ""
        tvResult.text = ""

        val base = etBase.text.toString().trim().uppercase()
        val dest = etDest.text.toString().trim().uppercase()
        val amountStr = etAmount.text.toString().trim()

        val errorMsg = validateInputs(base, dest, amountStr)
        if (errorMsg != null) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        val amount = amountStr.toDouble()

        btnConvert.isEnabled = false
        progress.visibility = View.VISIBLE

        val url =
            "https://api.freecurrencyapi.com/v1/latest?apikey=${API_KEY}&base_currency=${base}&currencies=${dest}"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    btnConvert.isEnabled = true
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    btnConvert.isEnabled = true
                }

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "API error: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val data = response.body?.string() ?: return
                val json = JSONObject(data)

                if (!json.has("data")) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Missing currency", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val rate = json.getJSONObject("data").getDouble(dest)
                val converted = rate * amount

                runOnUiThread {
                    tvRate.text = "1 $base = $rate $dest"
                    tvResult.text = "$amount $base = $converted $dest"
                }
            }
        })
    }

    private fun validateInputs(b: String, d: String, a: String): String? {
        val pattern = Pattern.compile("^[A-Z]{3}$")
        if (!pattern.matcher(b).matches()) return "Base currency invalid"
        if (!pattern.matcher(d).matches()) return "Destination currency invalid"
        if (a.isEmpty()) return "Amount required"
        if (a.toDoubleOrNull() == null || a.toDouble() <= 0.0) return "Invalid amount"
        return null
    }
}
