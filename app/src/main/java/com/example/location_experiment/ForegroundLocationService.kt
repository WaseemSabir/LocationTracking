package com.example.location_experiment

import android.Manifest.permission
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Binder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.location_experiment.ForegroundLocationService.LocalBinder
import com.example.location_experiment.data.LocationPreferences
import com.example.location_experiment.data.LocationRepository
import com.example.myapplication.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundLocationService : LifecycleService() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var locationPreferences: LocationPreferences

    private val localBinder = LocalBinder()
    private var bindCount = 0

    private var started = false
    private var isForeground = false

    private var wakeLock: PowerManager.WakeLock? = null

    private fun isBound() = bindCount > 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // This action comes from our ongoing notification. The user requested to stop updates.
        if (intent?.action == ACTION_STOP_UPDATES) {
            stopLocationUpdates()
            lifecycleScope.launch {
                locationPreferences.setLocationTurnedOn(false)
            }
        }

        // Startup tasks only happen once.
        if (!started) {
            started = true
            // Check if we should turn on location updates.
            lifecycleScope.launch {
                if (locationPreferences.isLocationTurnedOn.first()) {
                    if (hasPermission(permission.ACCESS_FINE_LOCATION) ||
                        hasPermission(permission.ACCESS_COARSE_LOCATION)
                    ) {
                        locationRepository.startLocationUpdates()
                    }
                }
            }

            // Update any foreground notification when we receive location updates.
            lifecycleScope.launch {
                showNotification(locationRepository.lastLocation.value)
            }
        }

        manageLifetime()

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        handleBind()
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        handleBind()
    }

    private fun handleBind() {
        bindCount++
        startService(Intent(this, this::class.java))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bindCount--
        lifecycleScope.launch {
            manageLifetime()
        }
        return true
    }

    private fun manageLifetime() {
        when {
            isBound() -> exitForeground()
            locationRepository.isReceivingLocationUpdates.value -> enterForeground()
            else -> stopSelf()
        }
    }

    private fun exitForeground() {
        if (isForeground) {
            isForeground = false
            stopForeground(true)
        }
    }

    private fun enterForeground() {
        if (!isForeground) {
            isForeground = true

            showNotification(locationRepository.lastLocation.value)
        }
    }

    private fun showNotification(location: Location?) {
        if (!isForeground) {
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(location))
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "location_notif_chan",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(location: Location?): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(this.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        PendingIntent.getService(
            this,
            0,
            Intent(this, this::class.java).setAction(ACTION_STOP_UPDATES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentText = if (location != null) {
            "Location: ${location.latitude}, ${location.longitude}"
        } else {
            "Waiting for location"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Getting Location")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.location_on_fill0_wght400_grad0_opsz48)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    fun startLocationUpdates() {
        // power lock
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ForegroundLocationService::lock"
                ).apply {
                    acquire()
                }
            }

        locationRepository.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        locationRepository.stopLocationUpdates()
    }

    fun toggleAccessibility() {
        locationRepository.toggleAccessible()
    }

    internal inner class LocalBinder : Binder() {
        fun getService(): ForegroundLocationService = this@ForegroundLocationService
    }

    private companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "LocationUpdates"
        const val ACTION_STOP_UPDATES = "MYAPP.ACTION_STOP_UPDATES"
    }
}

class ForegroundLocationServiceConnection @Inject constructor() : ServiceConnection {

    var service: ForegroundLocationService? = null
        private set

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = (binder as LocalBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        // Note: this should never be called since the service is in the same process.
        service = null
    }
}
