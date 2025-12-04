package com.example.a2_kelvin_shi

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class MainFragment : Fragment() {

    private lateinit var etBase: EditText
    private lateinit var etDest: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnConvert: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvRate: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnAbout: Button

    private val API_KEY = "fca_live_pfNc8LrSA99Kp8d1pbct7vFbRFmBynBPh1WoSMM4"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        etBase = view.findViewById(R.id.etBase)
        etDest = view.findViewById(R.id.etDest)
        etAmount = view.findViewById(R.id.etAmount)
        btnConvert = view.findViewById(R.id.btnConvert)
        progress = view.findViewById(R.id.progress)
        tvRate = view.findViewById(R.id.tvRate)
        tvResult = view.findViewById(R.id.tvResult)
        btnAbout = view.findViewById(R.id.btnAbout)

        btnConvert.setOnClickListener { performConversion() }
        btnAbout.setOnClickListener {
            findNavController().navigate(R.id.aboutFragment)
        }

        return view
    }

    private fun performConversion() {
        tvRate.text = ""
        tvResult.text = ""

        val base = etBase.text.toString().trim().uppercase()
        val dest = etDest.text.toString().trim().uppercase()
        val amountStr = etAmount.text.toString().trim()

        val errorMsg = validateInputs(base, dest, amountStr)
        if (errorMsg != null) {
            showToast(errorMsg)
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
                activity?.runOnUiThread {
                    progress.visibility = View.GONE
                    btnConvert.isEnabled = true
                    showToast("Network error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    progress.visibility = View.GONE
                    btnConvert.isEnabled = true
                }

                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        showToast("API error: ${response.code}")
                    }
                    return
                }

                val data = response.body?.string() ?: return
                val json = JSONObject(data)

                if (!json.has("data")) {
                    activity?.runOnUiThread {
                        showToast("Missing currency")
                    }
                    return
                }

                val rate = json.getJSONObject("data").getDouble(dest)
                val converted = rate * amount

                activity?.runOnUiThread {
                    tvRate.text = "1 $base = $rate $dest"
                    tvResult.text = "$amount $base = $converted $dest"
                }
            }
        })
    }

    private fun validateInputs(b: String, d: String, a: String): String? {
        val pattern = Pattern.compile("^[A-Z]{3}\$")
        if (!pattern.matcher(b).matches()) return "Base currency invalid"
        if (!pattern.matcher(d).matches()) return "Destination currency invalid"
        if (a.isEmpty()) return "Amount required"
        if (a.toDoubleOrNull() == null || a.toDouble() <= 0.0) return "Invalid amount"
        return null
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
