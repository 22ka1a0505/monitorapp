package com.example.appusageoverlay

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppLimitStore(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("AppLimits", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLimits(limits: List<AppLimit>) {
        val json = gson.toJson(limits)
        sharedPreferences.edit().putString("limits", json).apply()
    }

    fun loadLimits(): MutableList<AppLimit> {
        val json = sharedPreferences.getString("limits", "[]")
        val type = object : TypeToken<MutableList<AppLimit>>() {}.type
        return gson.fromJson(json, type)
    }
}
