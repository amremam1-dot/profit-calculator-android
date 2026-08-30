package com.example.profitcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.profitcalculator.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    private var currentTab = 0 // 0: Operation 1, 1: Operation 2
    private var isSwitchingTab = false
    private var lastCalculatedRawResult: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("profit_calc_prefs", Context.MODE_PRIVATE)

        setupTabs()
        setupListeners()
        loadCurrentTabData()
        renderHistory()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newTab = tab?.position ?: 0
                if (newTab != currentTab) {
                    saveCurrentTabData()
                    currentTab = newTab
                    loadCurrentTabData()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        binding.etLastAmount.doAfterTextChanged {
            if (!isSwitchingTab) {
                saveCurrentTabData()
                calculate()
            }
        }
        binding.etCapitalSay.doAfterTextChanged {
            if (!isSwitchingTab) {
                saveCurrentTabData()
                calculate()
            }
        }
        binding.etMonthProfits.doAfterTextChanged {
            if (!isSwitchingTab) {
                saveCurrentTabData()
                calculate()
            }
        }

        binding.btnClear.setOnClickListener {
            binding.etLastAmount.text?.clear()
            binding.etCapitalSay.text?.clear()
            binding.etMonthProfits.text?.clear()
            saveCurrentTabData()
            calculate()
            binding.etLastAmount.requestFocus()
        }

        // نسخ النتيجة بالضغط على بطاقة النتيجة
        binding.cardResult.setOnClickListener {
            copyToClipboard(binding.tvResultValue.text.toString())
        }

        binding.btnSaveHistory.setOnClickListener {
            saveOperationToHistory()
        }

        binding.btnClearHistory.setOnClickListener {
            clearHistory()
        }
    }

    private fun saveCurrentTabData() {
        val lastAmount = binding.etLastAmount.text?.toString().orEmpty()
        val capitalSay = binding.etCapitalSay.text?.toString().orEmpty()
        val monthProfits = binding.etMonthProfits.text?.toString().orEmpty()

        prefs.edit().apply {
            putString("tab_${currentTab}_last_amount", lastAmount)
            putString("tab_${currentTab}_capital_say", capitalSay)
            putString("tab_${currentTab}_month_profits", monthProfits)
            apply()
        }
    }

    private fun loadCurrentTabData() {
        isSwitchingTab = true
        val lastAmount = prefs.getString("tab_${currentTab}_last_amount", "") ?: ""
        val capitalSay = prefs.getString("tab_${currentTab}_capital_say", "") ?: ""
        val monthProfits = prefs.getString("tab_${currentTab}_month_profits", "") ?: ""

        binding.etLastAmount.setText(lastAmount)
        binding.etCapitalSay.setText(capitalSay)
        binding.etMonthProfits.setText(monthProfits)
        isSwitchingTab = false

        calculate()
    }

    private fun calculate() {
        val lastAmountStr = binding.etLastAmount.text?.toString()?.trim().orEmpty()
        val capitalSayStr = binding.etCapitalSay.text?.toString()?.trim().orEmpty()
        val monthProfitsStr = binding.etMonthProfits.text?.toString()?.trim().orEmpty()

        if (lastAmountStr.isEmpty() && capitalSayStr.isEmpty() && monthProfitsStr.isEmpty()) {
            binding.cardResult.strokeColor = ContextCompat.getColor(this, R.color.border_color)
            binding.cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
            binding.tvResultStatus.text = getString(R.string.enter_amounts_hint)
            binding.tvResultStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.tvResultValue.text = "0.00"
            binding.tvResultValue.setTextColor(ContextCompat.getColor(this, R.color.text_main))
            lastCalculatedRawResult = null
            return
        }

        val lastAmount = lastAmountStr.toDoubleOrNull() ?: 0.0
        val capitalSay = capitalSayStr.toDoubleOrNull() ?: 0.0
        val monthProfits = monthProfitsStr.toDoubleOrNull() ?: 0.0

        // المعادلة: المبلغ الأخير - كابيتال say - أرباح الشهر
        val result = lastAmount - capitalSay - monthProfits
        lastCalculatedRawResult = result

        val formattedValue = decimalFormat.format(Math.abs(result))

        when {
            result > 0 -> {
                val green = ContextCompat.getColor(this, R.color.profit_green)
                binding.cardResult.strokeColor = green
                binding.cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.profit_green_light))
                binding.tvResultStatus.text = getString(R.string.profit_status)
                binding.tvResultStatus.setTextColor(green)
                binding.tvResultValue.text = "+$formattedValue"
                binding.tvResultValue.setTextColor(green)
            }
            result < 0 -> {
                val red = ContextCompat.getColor(this, R.color.loss_red)
                binding.cardResult.strokeColor = red
                binding.cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.loss_red_light))
                binding.tvResultStatus.text = getString(R.string.loss_status)
                binding.tvResultStatus.setTextColor(red)
                binding.tvResultValue.text = "-$formattedValue"
                binding.tvResultValue.setTextColor(red)
            }
            else -> {
                binding.cardResult.strokeColor = ContextCompat.getColor(this, R.color.border_color)
                binding.cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
                binding.tvResultStatus.text = getString(R.string.break_even_status)
                binding.tvResultStatus.setTextColor(ContextCompat.getColor(this, R.color.text_main))
                binding.tvResultValue.text = "0.00"
                binding.tvResultValue.setTextColor(ContextCompat.getColor(this, R.color.text_main))
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Profit Calculator Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
    }

    private fun saveOperationToHistory() {
        val lastAmountStr = binding.etLastAmount.text?.toString()?.trim().orEmpty()
        val capitalSayStr = binding.etCapitalSay.text?.toString()?.trim().orEmpty()
        val monthProfitsStr = binding.etMonthProfits.text?.toString()?.trim().orEmpty()

        if (lastAmountStr.isEmpty() && capitalSayStr.isEmpty() && monthProfitsStr.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال مبالغ لحفظها", Toast.LENGTH_SHORT).show()
            return
        }

        val historyJson = prefs.getString("operations_history", "[]") ?: "[]"
        val jsonArray = JSONArray(historyJson)

        val newRecord = JSONObject().apply {
            put("tab", if (currentTab == 0) "العملية 1" else "العملية 2")
            put("lastAmount", lastAmountStr.ifEmpty { "0" })
            put("capitalSay", capitalSayStr.ifEmpty { "0" })
            put("monthProfits", monthProfitsStr.ifEmpty { "0" })
            put("result", binding.tvResultValue.text.toString())
            put("date", SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date()))
        }

        // إضافة في بداية المصفوفة
        val updatedArray = JSONArray()
        updatedArray.put(newRecord)
        for (i in 0 until jsonArray.length()) {
            if (i < 20) { // الاحتفاظ بآخر 20 عملية
                updatedArray.put(jsonArray.getJSONObject(i))
            }
        }

        prefs.edit().putString("operations_history", updatedArray.toString()).apply()
        renderHistory()
        Toast.makeText(this, getString(R.string.history_saved_toast), Toast.LENGTH_SHORT).show()
    }

    private fun renderHistory() {
        binding.layoutHistoryContainer.removeAllViews()
        val historyJson = prefs.getString("operations_history", "[]") ?: "[]"
        val jsonArray = JSONArray(historyJson)

        if (jsonArray.length() == 0) {
            binding.tvEmptyHistory.visibility = View.VISIBLE
            return
        }

        binding.tvEmptyHistory.visibility = View.GONE

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val tabName = item.optString("tab", "")
            val last = item.optString("lastAmount", "0")
            val capital = item.optString("capitalSay", "0")
            val month = item.optString("monthProfits", "0")
            val result = item.optString("result", "0.00")
            val date = item.optString("date", "")

            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(if (i % 2 == 0) ContextCompat.getColor(context, R.color.input_background) else Color.TRANSPARENT)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }

                // زر استرجاع أو نسخ
                setOnClickListener {
                    copyToClipboard(result)
                }
            }

            val tvInfo = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "[$tabName] $date\nالمبلغ: $last | كابيتال: $capital | الشهر: $month"
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                textSize = 11.5f
            }

            val tvRes = TextView(this).apply {
                text = result
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    if (result.startsWith("+")) ContextCompat.getColor(context, R.color.profit_green)
                    else if (result.startsWith("-")) ContextCompat.getColor(context, R.color.loss_red)
                    else ContextCompat.getColor(context, R.color.text_main)
                )
            }

            rowView.addView(tvInfo)
            rowView.addView(tvRes)
            binding.layoutHistoryContainer.addView(rowView)
        }
    }

    private fun clearHistory() {
        prefs.edit().remove("operations_history").apply()
        renderHistory()
        Toast.makeText(this, "تم مسح السجل", Toast.LENGTH_SHORT).show()
    }
}
