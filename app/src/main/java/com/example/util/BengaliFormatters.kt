package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BengaliFormatters {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bengaliMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun toBengaliDigits(value: Int): String = toBengaliDigits(value.toString())

    fun toBengaliDigits(value: Long): String = toBengaliDigits(value.toString())

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 0) return "০ বাইট"
        return when {
            bytes < 1024L -> "${toBengaliDigits(bytes)} বাইট"
            bytes < 1024L * 1024L -> {
                val kb = bytes / 1024.0
                val formatted = String.format(Locale.US, "%.1f", kb)
                "${toBengaliDigits(formatted)} কেবি"
            }
            bytes < 1024L * 1024L * 1024L -> {
                val mb = bytes / (1024.0 * 1024.0)
                val formatted = String.format(Locale.US, "%.2f", mb)
                "${toBengaliDigits(formatted)} এমবি"
            }
            else -> {
                val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                val formatted = String.format(Locale.US, "%.2f", gb)
                "${toBengaliDigits(formatted)} জিবি"
            }
        }
    }

    fun formatDateTime(timestampMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val day = toBengaliDigits(cal.get(Calendar.DAY_OF_MONTH))
        val month = bengaliMonths[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val year = toBengaliDigits(cal.get(Calendar.YEAR))
        val hour = toBengaliDigits(String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY)))
        val minute = toBengaliDigits(String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE)))
        return "$day $month, $year • $hour:$minute"
    }

    fun formatShortDate(timestampMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val day = toBengaliDigits(cal.get(Calendar.DAY_OF_MONTH))
        val month = bengaliMonths[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val year = toBengaliDigits(cal.get(Calendar.YEAR))
        return "$day $month, $year"
    }

    fun formatPageCount(pages: Int): String = "${toBengaliDigits(pages)} পৃষ্ঠা"

    fun formatOrdinalPage(indexOneBased: Int): String {
        val suffix = when (indexOneBased) {
            1 -> "ম"
            2, 3 -> "য়"
            4 -> "র্থ"
            5 -> "ম"
            6 -> "ষ্ঠ"
            7, 8, 9, 10 -> "ম"
            else -> "তম"
        }
        return "${toBengaliDigits(indexOneBased)}$suffix পৃষ্ঠা"
    }

    fun defaultPdfTitle(): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "দলিল_${fmt.format(Date())}"
    }

    fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .trim()
        return cleaned.ifEmpty { defaultPdfTitle() }
    }
}
