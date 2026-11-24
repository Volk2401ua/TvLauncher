package com.example.tvlauncher

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    enum class SortMode { ALPHA, FREQUENT, RECENT }

    private val sp: SharedPreferences = context.applicationContext
        .getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    var columns: Int
        get() = sp.getInt(KEY_COLUMNS, 0)
        set(v) = sp.edit().putInt(KEY_COLUMNS, v).apply()

    var sortMode: SortMode
        get() = SortMode.valueOf(sp.getString(KEY_SORT, SortMode.ALPHA.name)!!)
        set(v) = sp.edit().putString(KEY_SORT, v.name).apply()

    var use24h: Boolean
        get() = sp.getBoolean(KEY_24H, true)
        set(v) = sp.edit().putBoolean(KEY_24H, v).apply()

    var showDate: Boolean
        get() = sp.getBoolean(KEY_DATE, true)
        set(v) = sp.edit().putBoolean(KEY_DATE, v).apply()

    var largeUi: Boolean
        get() = sp.getBoolean(KEY_LARGE, false)
        set(v) = sp.edit().putBoolean(KEY_LARGE, v).apply()

    var needRefresh: Boolean
        get() = sp.getBoolean(KEY_REFRESH, false)
        set(v) = sp.edit().putBoolean(KEY_REFRESH, v).apply()

    var lastAppPkg: String?
        get() = sp.getString(KEY_LAST_PKG, null)
        set(v) = sp.edit().putString(KEY_LAST_PKG, v).apply()

    var lastAppActivity: String?
        get() = sp.getString(KEY_LAST_ACT, null)
        set(v) = sp.edit().putString(KEY_LAST_ACT, v).apply()

    var sleepEndAt: Long
        get() = sp.getLong(KEY_SLEEP_END, 0L)
        set(v) = sp.edit().putLong(KEY_SLEEP_END, v).apply()

    var ukrainianOrnament: Boolean
        get() = sp.getBoolean(KEY_ORNAMENT, true)
        set(v) = sp.edit().putBoolean(KEY_ORNAMENT, v).apply()

    var ukrainianWatermark: Boolean
        get() = sp.getBoolean(KEY_WATERMARK, true)
        set(v) = sp.edit().putBoolean(KEY_WATERMARK, v).apply()

    var accentIndex: Int
        get() = sp.getInt(KEY_ACCENT, 0).coerceIn(0, ACCENT_COLORS.size - 1)
        set(v) = sp.edit().putInt(KEY_ACCENT, v.coerceIn(0, ACCENT_COLORS.size - 1)).apply()

    var animations: Boolean
        get() = sp.getBoolean(KEY_ANIM, true)
        set(v) = sp.edit().putBoolean(KEY_ANIM, v).apply()

    fun accentColor(): Int = ACCENT_COLORS[accentIndex]

    fun clearSleepTimer() = sp.edit().remove(KEY_SLEEP_END).apply()
    fun sleepRemainingMs(): Long = (sleepEndAt - System.currentTimeMillis()).coerceAtLeast(0L)

    fun hiddenApps(): Set<String> = sp.getStringSet(KEY_HIDDEN, emptySet())!!.toSet()
    fun pinnedApps(): Set<String> = sp.getStringSet(KEY_PINNED, emptySet())!!.toSet()

    fun pinApp(pkg: String) = changeSet(KEY_PINNED, pkg, true)
    fun unpinApp(pkg: String) = changeSet(KEY_PINNED, pkg, false)
    fun hideApp(pkg: String) = changeSet(KEY_HIDDEN, pkg, true)
    fun unhideApp(pkg: String) = changeSet(KEY_HIDDEN, pkg, false)

    fun order(): List<String> = sp.getString(KEY_ORDER, "")!!.split("|").filter { it.isNotEmpty() }
    fun saveOrder(list: List<String>) =
        sp.edit().putString(KEY_ORDER, list.joinToString("|")).apply()

    private fun changeSet(key: String, pkg: String, add: Boolean) {
        val s = sp.getStringSet(key, emptySet())!!.toMutableSet()
        if (add) s.add(pkg) else s.remove(pkg)
        sp.edit().putStringSet(key, s).apply()
    }

    fun recordLaunch(pkg: String, activity: String) {
        val count = sp.getInt("usage_$pkg", 0) + 1
        sp.edit()
            .putInt("usage_$pkg", count)
            .putLong("last_$pkg", System.currentTimeMillis())
            .putString(KEY_LAST_PKG, pkg)
            .putString(KEY_LAST_ACT, activity)
            .apply()
    }

    fun usage(pkg: String): Int = sp.getInt("usage_$pkg", 0)
    fun lastUsed(pkg: String): Long = sp.getLong("last_$pkg", 0L)

    fun resetAll() { sp.edit().clear().apply() }

    companion object {
        private const val KEY_COLUMNS = "columns"
        private const val KEY_SORT = "sort"
        private const val KEY_24H = "use24h"
        private const val KEY_DATE = "showDate"
        private const val KEY_HIDDEN = "hidden_apps"
        private const val KEY_PINNED = "pinned_apps"
        private const val KEY_LARGE = "large_ui"
        private const val KEY_REFRESH = "need_refresh"
        private const val KEY_LAST_PKG = "last_pkg"
        private const val KEY_LAST_ACT = "last_act"
        private const val KEY_SLEEP_END = "sleep_end"
        private const val KEY_ORDER = "manual_order"
        private const val KEY_ACCENT = "accent"
        private const val KEY_ANIM = "animations"
        private const val KEY_ORNAMENT = "ua_ornament"
        private const val KEY_WATERMARK = "ua_watermark"

        val ACCENT_COLORS = intArrayOf(
            0xFFFFD700.toInt(),
            0xFF0057B7.toInt(),
            0xFF4FC3F7.toInt(),
            0xFFFFB74D.toInt(),
            0xFF81C784.toInt(),
            0xFFF48FB1.toInt(),
            0xFFBA68C8.toInt(),
        )
        val ACCENT_NAMES = intArrayOf(
            R.string.accent_ua_gold,
            R.string.accent_ua_blue,
            R.string.accent_cyan,
            R.string.accent_amber,
            R.string.accent_mint,
            R.string.accent_pink,
            R.string.accent_purple,
        )
    }
}