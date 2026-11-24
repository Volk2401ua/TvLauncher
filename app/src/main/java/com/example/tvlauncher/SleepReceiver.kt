package com.example.tvlauncher

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, SleepAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) dpm.lockNow()
        Prefs(context).clearSleepTimer()
    }
}