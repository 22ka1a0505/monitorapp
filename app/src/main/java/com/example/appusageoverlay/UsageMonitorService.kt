package com.example.appusageoverlay

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class UsageMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var store: AppLimitStore
    private lateinit var overlayManager: OverlayManager
    private var currentApp: String? = null

    override fun onCreate() {
        super.onCreate()
        store = AppLimitStore(this)
        overlayManager = OverlayManager(this)
        startForegroundService()
        startMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        val channelId = "UsageMonitorServiceChannel"
        val channelName = "App Usage Monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Usage Monitoring")
            .setContentText("Monitoring app usage in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, notification)
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null) {
                    val limits = store.loadLimits()
                    val appLimit = limits.find { it.packageName == foregroundApp }
                    if (appLimit != null) {
                        if (currentApp == foregroundApp) {
                            appLimit.usedMinutesToday++
                            store.saveLimits(limits)
                        } else {
                            currentApp = foregroundApp
                        }
                        if (appLimit.usedMinutesToday >= appLimit.timeLimitMinutes) {
                            overlayManager.showOverlay(appLimit.appName)
                        }
                    }
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10000
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var lastUsedApp: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastUsedApp = event.packageName
            }
        }
        return lastUsedApp
    }
}
