package com.v94studio.moneymanager.ui.util

import android.content.Context
import android.telephony.TelephonyManager
import java.util.*

object CurrencyDetector {
    /**
     * Detects the currency code based on the user's network or SIM region.
     * Fallback to Locale if telephony data is unavailable.
     */
    fun detectCurrencyCode(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        
        // 1. Try Network Country ISO (most accurate for current location)
        val networkCountry = telephonyManager?.networkCountryIso?.uppercase()
        if (!networkCountry.isNullOrBlank()) {
            val currency = getCurrencyFromCountryCode(networkCountry)
            if (currency != null) return currency
        }

        // 2. Try SIM Country ISO
        val simCountry = telephonyManager?.simCountryIso?.uppercase()
        if (!simCountry.isNullOrBlank()) {
            val currency = getCurrencyFromCountryCode(simCountry)
            if (currency != null) return currency
        }

        // 3. Fallback to System Locale
        return try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD" // Ultimate fallback
        }
    }

    private fun getCurrencyFromCountryCode(countryCode: String): String? {
        return try {
            // Special cases for specific regions
            when (countryCode) {
                "ZM" -> "ZMW" // Zambia
                "ZA" -> "ZAR" // South Africa
                else -> {
                    // General lookup using Locale
                    val locale = Locale("", countryCode)
                    Currency.getInstance(locale).currencyCode
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
