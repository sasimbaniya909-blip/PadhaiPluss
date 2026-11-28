package com.ps.padhaiplus

import java.util.Calendar

data class NepaliDate(
    val year: Int,
    val monthIndex: Int, // 0-based
    val day: Int,
    val monthName: String,
    val dayOfWeekName: String
)

object NepaliCalendarUtils {
    val nepaliMonths = listOf(
        "Baishakh (वैशाख)",
        "Jestha (जेठ)",
        "Asar (असार)",
        "Shrawan (साउन)",
        "Bhadra (भदौ)",
        "Ashwin (कात्तिक/असोज)",
        "Kartik (कात्तिक)",
        "Mangsir (मंसिर)",
        "Poush (पुस)",
        "Magh (माघ)",
        "Falgun (फागुन)",
        "Chaitra (चैत)"
    )

    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Average days in each BS month
    private val monthDays = listOf(31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30)

    fun getCurrentNepaliDate(): NepaliDate {
        val cal = Calendar.getInstance()
        val yearAD = cal.get(Calendar.YEAR)
        val monthAD = cal.get(Calendar.MONTH) // 0-based (0 = Jan)
        val dayAD = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun

        // Convert AD to approximate BS date (BS is ~56 years and 8.5 months ahead)
        var yearBS = yearAD + 56
        var monthBS = (monthAD + 8) % 12
        if (monthAD >= 3) {
            yearBS += 1
        }
        var dayBS = dayAD + 13
        val maxDays = monthDays[monthBS]
        if (dayBS > maxDays) {
            dayBS -= maxDays
            monthBS = (monthBS + 1) % 12
        }

        val monthName = nepaliMonths[monthBS]
        val dayOfWeekName = weekDays[dayOfWeekIndex]

        return NepaliDate(
            year = yearBS,
            monthIndex = monthBS,
            day = dayBS,
            monthName = monthName,
            dayOfWeekName = dayOfWeekName
        )
    }

    fun getDaysInMonth(monthIndex: Int, year: Int = 2081): Int {
        val safeIndex = (monthIndex % 12 + 12) % 12
        return monthDays[safeIndex]
    }

    fun getFirstDayOfWeekOffset(year: Int, monthIndex: Int): Int {
        val safeIndex = (monthIndex % 12 + 12) % 12
        return (year + safeIndex * 3) % 7
    }

    fun getGreetingTitle(name: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good Morning, $name"
            in 12..16 -> "Good Afternoon, $name"
            else -> "Good Evening, $name"
        }
    }
}
