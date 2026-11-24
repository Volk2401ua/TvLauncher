package com.example.tvlauncher

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var sortRow: TextView
    private lateinit var colsRow: TextView
    private lateinit var clockRow: TextView
    private lateinit var dateRow: TextView
    private lateinit var sizeRow: TextView
    private lateinit var accentRow: TextView
    private lateinit var animRow: TextView
    private lateinit var ornamentRow: TextView
    private lateinit var watermarkRow: TextView
    private lateinit var sleepRow: TextView
    private lateinit var screenOffRow: TextView
    private lateinit var refreshRow: TextView
    private lateinit var hiddenRow: TextView
    private lateinit var resetRow: TextView

    private var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        sortRow = findViewById(R.id.sortRow)
        colsRow = findViewById(R.id.colsRow)
        clockRow = findViewById(R.id.clockRow)
        dateRow = findViewById(R.id.dateRow)
        sizeRow = findViewById(R.id.sizeRow)
        accentRow = findViewById(R.id.accentRow)
        animRow = findViewById(R.id.animRow)
        ornamentRow = findViewById(R.id.ornamentRow)
        watermarkRow = findViewById(R.id.watermarkRow)
        sleepRow = findViewById(R.id.sleepRow)
        screenOffRow = findViewById(R.id.screenOffRow)
        refreshRow = findViewById(R.id.refreshRow)
        hiddenRow = findViewById(R.id.hiddenRow)
        resetRow = findViewById(R.id.resetRow)

        sortRow.setOnClickListener { pickSort() }
        colsRow.setOnClickListener { pickColumns() }
        clockRow.setOnClickListener { prefs.use24h = !prefs.use24h; refresh() }
        dateRow.setOnClickListener { prefs.showDate = !prefs.showDate; refresh() }
        sizeRow.setOnClickListener { prefs.largeUi = !prefs.largeUi; refresh() }
        accentRow.setOnClickListener { pickAccent() }
        animRow.setOnClickListener { prefs.animations = !prefs.animations; refresh() }
        ornamentRow.setOnClickListener { prefs.ukrainianOrnament = !prefs.ukrainianOrnament; refresh() }
        watermarkRow.setOnClickListener { prefs.ukrainianWatermark = !prefs.ukrainianWatermark; refresh() }
        sleepRow.setOnClickListener { pickSleep() }
        screenOffRow.setOnClickListener { screenOffNow() }
        refreshRow.setOnClickListener {
            prefs.needRefresh = true
            finish()
        }
        hiddenRow.setOnClickListener { showHiddenApps() }
        resetRow.setOnClickListener { confirmReset() }

        refresh()
        sortRow.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        val action = pendingAction ?: return
        if (isAdminActive()) {
            pendingAction = null
            action()
        }
    }

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(this, SleepAdminReceiver::class.java))
    }

    private fun ensureAdmin(action: () -> Unit) {
        if (isAdminActive()) { action(); return }
        pendingAction = action
        val admin = ComponentName(this, SleepAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explain))
        }
        startActivity(intent)
    }

    private fun refresh() {
        val sortName = when (prefs.sortMode) {
            Prefs.SortMode.ALPHA -> getString(R.string.sort_alpha)
            Prefs.SortMode.FREQUENT -> getString(R.string.sort_frequent)
            Prefs.SortMode.RECENT -> getString(R.string.sort_recent)
        }
        sortRow.text = getString(R.string.setting_sort, sortName)

        val cols = if (prefs.columns == 0) getString(R.string.cols_auto) else prefs.columns.toString()
        colsRow.text = getString(R.string.setting_columns, cols)

        clockRow.text = getString(R.string.setting_clock,
            getString(if (prefs.use24h) R.string.on_24h else R.string.on_12h))
        dateRow.text = getString(R.string.setting_date,
            getString(if (prefs.showDate) R.string.on else R.string.off))
        sizeRow.text = getString(R.string.setting_size,
            getString(if (prefs.largeUi) R.string.size_large else R.string.size_normal))

        accentRow.text = getString(R.string.setting_accent,
            getString(Prefs.ACCENT_NAMES[prefs.accentIndex]))
        animRow.text = getString(R.string.setting_anim,
            getString(if (prefs.animations) R.string.on else R.string.off))

        ornamentRow.text = getString(R.string.setting_ornament,
            getString(if (prefs.ukrainianOrnament) R.string.on else R.string.off))
        watermarkRow.text = getString(R.string.setting_watermark,
            getString(if (prefs.ukrainianWatermark) R.string.on else R.string.off))

        val remaining = prefs.sleepRemainingMs()
        val sleepText = if (remaining > 0) {
            val min = (remaining / 60_000L).toInt().coerceAtLeast(1)
            getString(R.string.sleep_active, min)
        } else getString(R.string.sleep_off)
        sleepRow.text = getString(R.string.setting_sleep, sleepText)

        screenOffRow.text = getString(R.string.setting_screen_off)
        refreshRow.text = getString(R.string.setting_refresh)
        hiddenRow.text = getString(R.string.setting_hidden, prefs.hiddenApps().size)
        resetRow.text = getString(R.string.setting_reset)
    }

    private fun pickSort() {
        val modes = Prefs.SortMode.values()
        val labels = arrayOf(
            getString(R.string.sort_alpha),
            getString(R.string.sort_frequent),
            getString(R.string.sort_recent)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.setting_sort_title)
            .setSingleChoiceItems(labels, modes.indexOf(prefs.sortMode)) { dialog, which ->
                prefs.sortMode = modes[which]; refresh(); dialog.dismiss()
            }
            .show()
    }

    private fun pickColumns() {
        val labels = arrayOf(getString(R.string.cols_auto), "3", "4", "5", "6")
        val values = intArrayOf(0, 3, 4, 5, 6)
        AlertDialog.Builder(this)
            .setTitle(R.string.setting_columns_title)
            .setSingleChoiceItems(labels, values.indexOf(prefs.columns).coerceAtLeast(0)) { dialog, which ->
                prefs.columns = values[which]; refresh(); dialog.dismiss()
            }
            .show()
    }

    private fun pickAccent() {
        val labels = Prefs.ACCENT_NAMES.map { getString(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.setting_accent_title)
            .setSingleChoiceItems(labels, prefs.accentIndex) { dialog, which ->
                prefs.accentIndex = which; refresh(); dialog.dismiss()
            }
            .show()
    }

    private fun pickSleep() {
        val labels = arrayOf(
            getString(R.string.sleep_off),
            getString(R.string.sleep_30),
            getString(R.string.sleep_60),
            getString(R.string.sleep_90)
        )
        val values = intArrayOf(0, 30, 60, 90)
        val current = if (prefs.sleepRemainingMs() > 0) -1 else 0
        AlertDialog.Builder(this)
            .setTitle(R.string.setting_sleep_title)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                applySleep(values[which]); refresh(); dialog.dismiss()
            }
            .show()
    }

    private fun applySleep(minutes: Int) {
        ensureAdmin {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                this, 1001,
                Intent(this, SleepReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (minutes <= 0) {
                am.cancel(pi); prefs.clearSleepTimer()
                Toast.makeText(this, R.string.sleep_cancelled, Toast.LENGTH_SHORT).show()
            } else {
                val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                prefs.sleepEndAt = triggerAt
                Toast.makeText(this, getString(R.string.sleep_set, minutes), Toast.LENGTH_SHORT).show()
            }
            refresh()
        }
    }

    private fun screenOffNow() {
        ensureAdmin {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        }
    }

    private fun showHiddenApps() {
        val hidden = prefs.hiddenApps()
        if (hidden.isEmpty()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.no_hidden)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val pm = packageManager
        val list = hidden.map { pkg ->
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull() ?: pkg
            label to pkg
        }.sortedBy { it.first.lowercase() }

        AlertDialog.Builder(this)
            .setTitle(R.string.hidden_title)
            .setItems(list.map { it.first }.toTypedArray()) { _, which ->
                prefs.unhideApp(list[which].second); refresh(); showHiddenApps()
            }
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_reset)
            .setPositiveButton(android.R.string.ok) { _, _ -> prefs.resetAll(); refresh() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}