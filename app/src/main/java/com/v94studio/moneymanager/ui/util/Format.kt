package com.v94studio.moneymanager.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(amount: Double, currencyCode: String? = null): String {
    val locale = Locale.getDefault()
    val formatter = NumberFormat.getCurrencyInstance(locale)
    
    if (currencyCode != null) {
        try {
            val currency = Currency.getInstance(currencyCode)
            formatter.currency = currency
            
            if (formatter is DecimalFormat) {
                val symbols = formatter.decimalFormatSymbols
                var symbol = currency.getSymbol(locale)
                
                // If the symbol returned is just the 3-letter code, try to find a better one
                if (symbol == currencyCode || symbol.length > 2) {
                    // 1. Try US locale (common for many symbols like $)
                    val usSymbol = currency.getSymbol(Locale.US)
                    if (usSymbol.length <= 2 && usSymbol != currencyCode) {
                        symbol = usSymbol
                    } else {
                        // 2. Try to find a locale that matches the currency's first two letters
                        try {
                            val countryCode = currencyCode.substring(0, 2)
                            val countryLocale = Locale("", countryCode)
                            val fallbackSymbol = currency.getSymbol(countryLocale)
                            if (fallbackSymbol.length <= 2 && fallbackSymbol != currencyCode) {
                                symbol = fallbackSymbol
                            }
                        } catch (e: Exception) { /* ignore */ }
                    }
                }

                // 3. Last resort: Hardcoded common overrides for African and other currencies
                if (symbol == currencyCode || symbol.length > 2) {
                    symbol = when (currencyCode.uppercase()) {
                        "ZAR" -> "R"
                        "ZMW", "ZMK" -> "K"
                        "NGN" -> "₦"
                        "GHS" -> "GH₵"
                        "KES" -> "KSh"
                        "UGX" -> "USh"
                        "BWP" -> "P"
                        "MUR" -> "₨"
                        "ETB" -> "Br"
                        "TZS" -> "TSh"
                        "RWF" -> "FRw"
                        "MWK" -> "MK"
                        "SZL" -> "L"
                        "LSL" -> "L"
                        "NAD" -> "$"
                        "USD" -> "$"
                        "CAD" -> "$"
                        "AUD" -> "$"
                        "NZD" -> "$"
                        "HKD" -> "$"
                        "SGD" -> "$"
                        "GBP" -> "£"
                        "EUR" -> "€"
                        "JPY", "CNY" -> "¥"
                        "INR" -> "₹"
                        else -> symbol
                    }
                }
                
                symbols.currencySymbol = symbol
                formatter.decimalFormatSymbols = symbols
            }
        } catch (e: Exception) {
            // fallback to default
        }
    }

    return formatter.format(amount)
}

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatTime(timestamp: Long, is24Hour: Boolean): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long, is24Hour: Boolean): String {
    val p = if (is24Hour) "MMM d, yyyy - HH:mm" else "MMM d, yyyy - h:mm a"
    val f = SimpleDateFormat(p, Locale.getDefault())
    return f.format(Date(timestamp))
}

fun mergeDateAndTime(dateMillis: Long, timeMillis: Long): Long {
    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val timeCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
    dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
    dateCal.set(Calendar.SECOND, 0)
    dateCal.set(Calendar.MILLISECOND, 0)
    return dateCal.timeInMillis
}

fun updateTime(timestamp: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun daysToMillis(days: Int): Long = days * 24L * 60L * 60L * 1000L

fun formatSignedAmount(amount: Double, currencyCode: String? = null): String {
    val formatted = formatCurrency(abs(amount), currencyCode)
    return if (amount >= 0) "+$formatted" else "-$formatted"
}

fun getLocalizedCategory(name: String, context: android.content.Context): String {
    val resId = when (name.lowercase()) {
        "salary" -> com.v94studio.moneymanager.R.string.cat_salary
        "freelance" -> com.v94studio.moneymanager.R.string.cat_freelance
        "food" -> com.v94studio.moneymanager.R.string.cat_food
        "rent" -> com.v94studio.moneymanager.R.string.cat_rent
        "transport" -> com.v94studio.moneymanager.R.string.cat_transport
        "utilities" -> com.v94studio.moneymanager.R.string.cat_utilities
        "shopping" -> com.v94studio.moneymanager.R.string.cat_shopping
        "uncategorized" -> com.v94studio.moneymanager.R.string.cat_uncategorized
        else -> null
    }
    return if (resId != null) context.getString(resId) else name
}

fun getCategoryEmoji(name: String?): String {
    return when (name?.lowercase()) {
        "salary", "income" -> "💰"
        "freelance", "business" -> "💼"
        "food", "dining", "food & dining", "lunch", "dinner" -> "🍲"
        "coffee", "cafe" -> "☕"
        "rent", "housing" -> "🏠"
        "transport", "taxi" -> "🚗"
        "fuel", "gas" -> "⛽"
        "shopping", "groceries" -> "🛒"
        "entertainment", "fun" -> "🎮"
        "education", "school", "books" -> "🎓"
        "health", "medical" -> "🏥"
        "mobile", "internet", "phone" -> "📱"
        "travel", "flight", "hotel" -> "✈️"
        "gifts", "donation" -> "🎁"
        "utilities", "electricity", "water" -> "⚡"
        "transfer" -> "🏦"
        "subscription", "netflix", "spotify" -> "💳"
        else -> "📦"
    }
}

fun getCategoryColor(name: String?): Color {
    return when (name?.lowercase()) {
        "salary", "income" -> Color(0xFF22C55E)
        "freelance", "business" -> Color(0xFF3B82F6)
        "food", "dining", "food & dining", "lunch", "dinner" -> Color(0xFFF59E0B)
        "coffee", "cafe" -> Color(0xFF78350F)
        "rent", "housing" -> Color(0xFF6366F1)
        "transport", "taxi" -> Color(0xFF8B5CF6)
        "fuel", "gas" -> Color(0xFFEF4444)
        "shopping", "groceries" -> Color(0xFFEC4899)
        "entertainment", "fun" -> Color(0xFF06B6D4)
        "education", "school", "books" -> Color(0xFF10B981)
        "health", "medical" -> Color(0xFFF43F5E)
        "mobile", "internet", "phone" -> Color(0xFF64748B)
        "travel", "flight", "hotel" -> Color(0xFFF97316)
        "gifts", "donation" -> Color(0xFFD946EF)
        "utilities", "electricity", "water" -> Color(0xFFEAB308)
        "transfer" -> Color(0xFF14B8A6)
        "subscription", "netflix", "spotify" -> Color(0xFF475569)
        else -> Color(0xFF94A3B8)
    }
}

fun getCategoryIcon(name: String?): ImageVector {
    return when (name?.lowercase()) {
        "salary", "income" -> Icons.Default.AttachMoney
        "freelance", "business" -> Icons.Default.BusinessCenter
        "food", "dining", "food & dining", "lunch", "dinner" -> Icons.Default.Restaurant
        "coffee", "cafe" -> Icons.Default.Coffee
        "rent", "housing" -> Icons.Default.Home
        "transport", "taxi" -> Icons.Default.DirectionsCar
        "fuel", "gas" -> Icons.Default.LocalGasStation
        "shopping", "groceries" -> Icons.Default.ShoppingCart
        "entertainment", "fun" -> Icons.Default.Gamepad
        "education", "school", "books" -> Icons.Default.School
        "health", "medical" -> Icons.Default.MedicalServices
        "mobile", "internet", "phone" -> Icons.Default.PhoneAndroid
        "travel", "flight", "hotel" -> Icons.Default.Flight
        "gifts", "donation" -> Icons.Default.CardGiftcard
        "utilities", "electricity", "water" -> Icons.Default.FlashOn
        "transfer" -> Icons.Default.AccountBalance
        "subscription", "netflix", "spotify" -> Icons.Default.CreditCard
        else -> Icons.Default.Category
    }
}
