package com.sion.laundry

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

object Store {
    private const val PREF = "laundry_store"
    private const val KEY = "items"
    val items = MutableStateFlow<List<Item>>(emptyList())

    fun load(ctx: Context) {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Item(
                    o.getString("id"), o.getString("name"), o.getString("category"),
                    o.getInt("threshold"), o.getLong("start")
                )
            )
        }
        items.value = list
    }

    private fun save(ctx: Context) {
        val arr = JSONArray()
        items.value.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("category", it.category)
                put("threshold", it.threshold); put("start", it.startEpochDay)
            })
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, name: String, category: String, threshold: Int) {
        items.value = items.value + Item(UUID.randomUUID().toString(), name, category, threshold, LocalDate.now().toEpochDay())
        save(ctx)
    }

    fun update(ctx: Context, item: Item, name: String, category: String, threshold: Int) {
        items.value = items.value.map { if (it.id == item.id) it.copy(name = name, category = category, threshold = threshold) else it }
        save(ctx)
    }

    fun wash(ctx: Context, item: Item) {   // 换洗后归零，自动刷新
        items.value = items.value.map { if (it.id == item.id) it.copy(startEpochDay = LocalDate.now().toEpochDay()) else it }
        save(ctx)
    }

    fun remove(ctx: Context, item: Item) {
        items.value = items.value.filter { it.id != item.id }
        save(ctx)
    }
}
