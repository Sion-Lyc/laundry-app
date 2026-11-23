package com.sion.laundry

import java.time.LocalDate

data class Item(
    val id: String,
    val name: String,
    val category: String,
    val threshold: Int,
    val startEpochDay: Long   // 上次换洗的日期，天数 = 今天 - 它，自动累加
) {
    val daysWorn: Int
        get() = (LocalDate.now().toEpochDay() - startEpochDay).toInt().coerceAtLeast(0)
    val due: Boolean get() = daysWorn >= threshold
    val lastWash: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
}
