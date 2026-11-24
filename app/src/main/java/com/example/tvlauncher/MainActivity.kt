package com.example.tvlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var appGrid: RecyclerView
    private lateinit var pinnedRow: RecyclerView
    private lateinit var pinnedContainer: View
    private lateinit var continueCard: LinearLayout
    private lateinit var continueIcon: ImageView
    private lateinit var continueApp: TextView
    private lateinit var sleepIndicator: TextView
    private lateinit var clock: TextClock
    private lateinit var dateView: TextView
    private lateinit var searchBtn: Button
    private lateinit var wifiBtn: Button
    private lateinit var settingsBtn: Button

    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppInfo> = emptyList()
    private var query: String = ""
    private var adapterLarge: Boolean? = null
    private var adapterAccent: Int? = null

    private val dateTick = object : Runnable {
        override fun run() {
            val now = Date()
            dateView.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            updateSleepIndicator()
            handler.postDelayed(this, 60_000L - now.time % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        appGrid = findViewById(R.id.appGrid)
        pinnedRow = findViewById(R.id.pinnedRow)
        pinnedContainer = findViewById(R.id.pinnedContainer)
        continueCard = findViewById(R.id.continueCard)
        continueIcon = findViewById(R.id.continueIcon)
        continueApp = findViewById(R.id.continueApp)
        sleepIndicator = findViewById(R.id.sleepIndicator)
        clock = findViewById(R.id.clock)
        dateView = findViewById(R.id.date)
        searchBtn = findViewById(R.id.searchBtn)
        wifiBtn = findViewById(R.id.wifiBtn)
        settingsBtn = findViewById(R.id.settingsBtn)

        pinnedRow.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        pinnedRow.setHasFixedSize(true)
        appGrid.setHasFixedSize(true)
        if (appGrid.layoutManager == null) appGrid.layoutManager = GridLayoutManager(this, 4)

        settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        wifiBtn.setOnClickListener { openWifi() }
        searchBtn.setOnClickListener { showSearchDialog() }

        setupUkrainianTheme()
        loadAppsAsync()
    }

    override fun onResume() {
        super.onResume()
        setupUkrainianTheme()
        applyLayoutSettings()
        ensureAdapters()
        if (prefs.needRefresh) {
            prefs.needRefresh = false
            loadAppsAsync()
        } else if (allApps.isNotEmpty()) {
            rebuildLists(requestFocus = false)
        }
        updateContinueCard()
        updateSleepIndicator()
        handler.post(dateTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(dateTick)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* лаунчер — домашний экран */ }

    private fun setupUkrainianTheme() {
        val ornamentView = findViewById<View>(R.id.bgOrnament)
        if (prefs.ukrainianOrnament) {
            val tilePx = (64 * resources.displayMetrics.density).toInt()
            val tile = UiHelpers.TiledDrawable(
                resources.getDrawable(R.drawable.ornament_tile, theme),
                tilePx
            )
            tile.alpha = 18
            ornamentView.background = tile
        } else {
            ornamentView.background = null
        }

        findViewById<View>(R.id.watermark).visibility =
            if (prefs.ukrainianWatermark) View.VISIBLE else View.GONE
    }

    private fun applyLayoutSettings() {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        val cols = if (prefs.columns > 0) prefs.columns else (widthDp / 180f).toInt().coerceAtLeast(3)
        val lm = appGrid.layoutManager as? GridLayoutManager
        if (lm == null || lm.spanCount != cols) {
            appGrid.layoutManager = GridLayoutManager(this, cols)
        }
        clock.format12Hour = "h:mm a"
        clock.format24Hour = if (prefs.use24h) "HH:mm" else "h:mm a"
        dateView.visibility = if (prefs.showDate) View.VISIBLE else View.GONE

        val accent = prefs.accentColor()
        wifiBtn.background = UiHelpers.ghostButton(this, accent)
        searchBtn.background = UiHelpers.ghostButton(this, accent)
        settingsBtn.background = UiHelpers.ghostButton(this, accent)
        continueCard.background = UiHelpers.tile(this, accent)
    }

    private fun ensureAdapters() {
        val large = prefs.largeUi
        val accent = prefs.accentColor()
        val animations = prefs.animations
        if (adapterLarge == large && adapterAccent == accent) {
            (appGrid.adapter as? AppAdapter)?.animations = animations
            (pinnedRow.adapter as? AppAdapter)?.animations = animations
            return
        }
        adapterLarge = large
        adapterAccent = accent
        pinnedRow.adapter = AppAdapter(large, accent, animations, { launchApp(it) }, { showAppMenu(it) })
        appGrid.adapter = AppAdapter(large, accent, animations, { launchApp(it) }, { showAppMenu(it) })
    }

    private fun loadAppsAsync() {
        Thread {
            val apps = queryApps()
            runOnUiThread {
                allApps = apps
                rebuildLists(requestFocus = true)
            }
        }.start()
    }

    private fun rebuildLists(requestFocus: Boolean) {
        val hidden = prefs.hiddenApps()
        val pinnedSet = prefs.pinnedApps()
        val filtered = allApps.asSequence()
            .filter { it.packageName !in hidden }
            .filter { query.isEmpty() || it.label.contains(query, ignoreCase = true) }
            .toList()

        val pinned = sort(filtered.filter { it.packageName in pinnedSet })
        val rest = sort(filtered.filter { it.packageName !in pinnedSet })

        (pinnedRow.adapter as? AppAdapter)?.submit(pinned)
        (appGrid.adapter as? AppAdapter)?.submit(rest)
        pinnedContainer.visibility = if (pinned.isEmpty()) View.GONE else View.VISIBLE

        if (requestFocus) {
            appGrid.post {
                val target = if (pinned.isNotEmpty()) pinnedRow.getChildAt(0) else appGrid.getChildAt(0)
                target?.requestFocus()
            }
        }
    }

    private fun sort(list: List<AppInfo>): List<AppInfo> {
        val orderMap = prefs.order().withIndex().associate { it.value to it.index }
        val ordered = list.filter { it.packageName in orderMap }
            .sortedBy { orderMap[it.packageName] }
        val rest = list.filter { it.packageName !in orderMap }
        val sortedRest = when (prefs.sortMode) {
            Prefs.SortMode.ALPHA -> rest.sortedBy { it.label.lowercase(Locale.getDefault()) }
            Prefs.SortMode.FREQUENT -> rest.sortedWith(
                compareByDescending<AppInfo> { it.usage }.thenBy { it.label.lowercase(Locale.getDefault()) }
            )
            Prefs.SortMode.RECENT -> rest.sortedWith(
                compareByDescending<AppInfo> { it.lastUsed }.thenBy { it.label.lowercase(Locale.getDefault()) }
            )
        }
        return ordered + sortedRest
    }

    private fun currentDisplayOrder(): List<String> {
        val hidden = prefs.hiddenApps()
        val pinnedSet = prefs.pinnedApps()
        val filtered = allApps.filter { it.packageName !in hidden }
        val pinned = sort(filtered.filter { it.packageName in pinnedSet })
        val rest = sort(filtered.filter { it.packageName !in pinnedSet })
        return (pinned + rest).map { it.packageName }
    }

    private fun queryApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        val pinnedSet = prefs.pinnedApps()
        return infos.asSequence()
            .filter { it.activityInfo.packageName != packageName }
            .map {
                val pkg = it.activityInfo.packageName
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = pkg,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(pm),
                    pinned = pkg in pinnedSet,
                    usage = prefs.usage(pkg),
                    lastUsed = prefs.lastUsed(pkg)
                )
            }
            .toList()
    }

    private fun launchApp(app: AppInfo) {
        prefs.recordLaunch(app.packageName, app.activityName)
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        runCatching { startActivity(intent) }
        query = ""
        searchBtn.text = getString(R.string.search)
    }

    private fun updateContinueCard() {
        val pkg = prefs.lastAppPkg
        val act = prefs.lastAppActivity
        if (pkg == null || act == null) { continueCard.visibility = View.GONE; return }
        val appInfo = runCatching { packageManager.getApplicationInfo(pkg, 0) }.getOrNull()
        if (appInfo == null) { continueCard.visibility = View.GONE; return }
        continueCard.visibility = View.VISIBLE
        continueIcon.setImageDrawable(runCatching { packageManager.getApplicationIcon(appInfo) }.getOrNull())
        continueApp.text = runCatching {
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull() ?: pkg
        continueCard.setOnClickListener {
            prefs.recordLaunch(pkg, act)
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(pkg, act)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            runCatching { startActivity(intent) }
        }
    }

    private fun updateSleepIndicator() {
        val remaining = prefs.sleepRemainingMs()
        if (remaining > 0) {
            val min = (remaining / 60_000L).toInt().coerceAtLeast(1)
            sleepIndicator.visibility = View.VISIBLE
            sleepIndicator.text = getString(R.string.sleep_indicator, min)
        } else {
            sleepIndicator.visibility = View.GONE
        }
    }

    private fun showAppMenu(app: AppInfo) {
        val pinned = app.packageName in prefs.pinnedApps()
        val labels = arrayOf(
            if (pinned) getString(R.string.menu_unpin) else getString(R.string.menu_pin),
            getString(R.string.menu_hide),
            getString(R.string.menu_move_up),
            getString(R.string.menu_move_down),
            getString(R.string.menu_info),
            getString(R.string.menu_uninstall)
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> {
                        if (pinned) prefs.unpinApp(app.packageName) else prefs.pinApp(app.packageName)
                        rebuildLists(requestFocus = true)
                    }
                    1 -> {
                        prefs.hideApp(app.packageName)
                        allApps = allApps.filterNot { it.packageName == app.packageName }
                        rebuildLists(requestFocus = true)
                    }
                    2 -> { moveInOrder(app, up = true); rebuildLists(requestFocus = true) }
                    3 -> { moveInOrder(app, up = false); rebuildLists(requestFocus = true) }
                    4 -> openAppInfo(app.packageName)
                    5 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }

    private fun moveInOrder(app: AppInfo, up: Boolean) {
        val visible = currentDisplayOrder()
        val order = prefs.order().toMutableList()
        order.retainAll(visible.toSet())
        for (p in visible) if (p !in order) order.add(p)
        val idx = order.indexOf(app.packageName)
        val target = if (up) idx - 1 else idx + 1
        if (idx >= 0 && target in order.indices) {
            val tmp = order[idx]
            order[idx] = order[target]
            order[target] = tmp
            prefs.saveOrder(order)
        }
    }

    private fun openWifi() {
        val actions = listOf(
            Settings.ACTION_WIFI_SETTINGS,
            Settings.ACTION_WIRELESS_SETTINGS,
            Settings.ACTION_SETTINGS
        )
        for (action in actions) {
            try {
                startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {}
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.search_hint)
            setText(query)
            setSelection(text.length)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.search)
            .setView(input)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                query = ""
                searchBtn.text = getString(R.string.search)
                rebuildLists(requestFocus = true)
            }
            .setPositiveButton(android.R.string.ok, null)
            .create()

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                searchBtn.text = if (query.isEmpty()) getString(R.string.search)
                    else getString(R.string.search_active, query)
                rebuildLists(requestFocus = false)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.setOnDismissListener { input.requestFocus() }
        dialog.show()
        input.requestFocus()
    }

    private fun openAppInfo(pkg: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    private fun uninstallApp(pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE)
            .setData(Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }
}