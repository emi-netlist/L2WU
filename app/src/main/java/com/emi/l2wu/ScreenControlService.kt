package com.emi.l2wu
import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.emi.l2wu.repository.ServiceTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow

class ScreenControlService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var powerManager: PowerManager
//    private lateinit var wakeLock: PowerManager.WakeLock

    // This keeps the CPU "alive" when the screen is off
    private var cpuWakeLock: PowerManager.WakeLock? = null

    // This turns the screen ON
    private lateinit var screenWakeLock: PowerManager.WakeLock

    private val CHANNEL_ID = "ScreenControlChannel"
    private val NOTIF_ID = 1

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        // Prepare WakeLock to turn on screen
//        wakeLock = powerManager.newWakeLock(
//            @SuppressWarnings("deprecation") PowerManager.SCREEN_BRIGHT_WAKE_LOCK or @SuppressWarnings("deprecation") PowerManager.ACQUIRE_CAUSES_WAKEUP,
////            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or android.R.attr.turnScreenOn,
//            "ScreenControl:WakeUp"
//        )

        // 1. Partial Wake Lock: Keeps the CPU running to process sensor data
        cpuWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ScreenControl:CpuKeepAlive"
        )

        // 2. Screen Wake Lock: To actually turn the screen on
        screenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ScreenControl:WakeUp"
        )

        createNotificationChannel()

        // Mark as running as soon as the process creates the service
        ServiceTrackerRepository.setServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            LockAccessibilityService.instance?.lockScreen()
        }

        createNotificationChannel()
        showNotification()

        // Acquire the CPU lock so the sensor listener doesn't die
        if (cpuWakeLock?.isHeld == false) {
            cpuWakeLock?.acquire()
        }

        // Register sensor with a delay that is less likely to be throttled
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL   // NORMAL to save battery/prevent throttling
        )

//        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        return START_STICKY
    }

    private fun showNotification() {
        val lockIntent = Intent(this, ScreenControlService::class.java).apply {
            action = "LOCK_SCREEN"
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, lockIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Screen Control Service")
            .setContentText("Tap here to lock the screen instantly.")
            .setPriority(NotificationCompat.PRIORITY_MAX) // High priority
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent) // Clicking the notification triggers the lock logic
            .build()

        startForeground(NOTIF_ID, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]
            val z = event.values[2]

            // Simple logic: If phone is tilted up (Y increases) and screen is off
            if (y > 3.9 /*&& !powerManager.isInteractive*/) {
//                wakeLock.acquire(1000) // Wake screen for 1 second
                if (!screenWakeLock.isHeld) {
                    // Turn screen on for 3 seconds
                    screenWakeLock.acquire(3000)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Controller Service",
                NotificationManager.IMPORTANCE_HIGH // Use high importance for visibility
            ).apply {
                description = "Provides a notification to lock the screen"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
//        wakeLock.release()
        if (cpuWakeLock?.isHeld == true) cpuWakeLock?.release()

        // Mark as stopped when the service dies
        ServiceTrackerRepository.setServiceRunning(false)
        super.onDestroy()
    }
}