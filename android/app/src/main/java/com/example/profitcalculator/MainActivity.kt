package com.example.profitcalculator

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.profitcalculator.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.etLastAmount.doAfterTextChanged { calculate() }
        binding.etCapitalSay.doAfterTextChanged { calculate() }
        binding.etMonthProfits.doAfterTextChanged { calculate() }

        binding.btnClear.setOnClickListener {
            binding.etLastAmount.text?.clear()
            binding.etCapitalSay.text?.clear()
            binding.etMonthProfits.text?.clear()
            binding.etLastAmount.requestFocus()
        }
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
            return
        }

        val lastAmount = lastAmountStr.toDoubleOrNull() ?: 0.0
        val capitalSay = capitalSayStr.toDoubleOrNull() ?: 0.0
        val monthProfits = monthProfitsStr.toDoubleOrNull() ?: 0.0

        // المعادلة: المبلغ الأخير - كابيتال say - أرباح الشهر
        val result = lastAmount - capitalSay - monthProfits

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
}
