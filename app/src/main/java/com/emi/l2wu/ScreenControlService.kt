package com.emi.l2wu
import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.emi.l2wu.repository.ServiceTrackerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

class ScreenControlService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private lateinit var powerManager: PowerManager
//    private lateinit var wakeLock: PowerManager.WakeLock

    // This keeps the CPU "alive" when the screen is off
    private var cpuWakeLock: PowerManager.WakeLock? = null

    // This turns the screen ON
    private lateinit var screenWakeLock: PowerManager.WakeLock

    private val CHANNEL_ID = "ScreenControlChannel"
    private val NOTIF_ID = 1

    // State variables holding the latest readings
    private var proximityDistance: Float = 0.0F
    private var accelX: Float = 0f
    private var accelY: Float = 0f
    private var accelZ: Float = 0f

    // Receiver to detect screen state changes
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen is OFF: Start listening for "lift"
                    startLiftingDetection()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Screen is on: Stop sensor to save power
                    stopLiftingDetection()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        // Prepare WakeLock to turn on screen
//        wakeLock = powerManager.newWakeLock(
//            @SuppressWarnings("deprecation") PowerManager.SCREEN_BRIGHT_WAKE_LOCK or @SuppressWarnings("deprecation") PowerManager.ACQUIRE_CAUSES_WAKEUP,
////            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or android.R.attr.turnScreenOn,
//            "ScreenControl:WakeUp"
//        )

        // Partial Wake Lock: Keeps the CPU running to process sensor data
        cpuWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ScreenControl:CpuKeepAlive"
        )

        // Screen Wake Lock: To actually turn the screen on
        screenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ScreenControl:WakeUp"
        )

        // Register the receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)

        // Initial state: if screen is already off, start detection
        if (!powerManager.isInteractive) {
            startLiftingDetection()
        }

        createNotificationChannel()
        // Mark as running as soon as the process creates the service
        ServiceTrackerRepository.setServiceRunning(true)
    }

    private fun startLiftingDetection(): Unit {
        if (cpuWakeLock?.isHeld == false) cpuWakeLock?.acquire()

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopLiftingDetection() {
        if (cpuWakeLock?.isHeld == true) cpuWakeLock?.release()
        sensorManager.unregisterListener(this)
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            LockAccessibilityService.instance?.lockScreen()
        }
        showNotification()
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
        val sensorEvent = event ?: return

        // 1. Update the latest state based on which sensor fired
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
//                val distance = sensorEvent.values[0]
//                val maxRange = proximitySensor?.maximumRange ?: 0f
//                isNear = distance < maxRange
                proximityDistance = sensorEvent.values[0]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = sensorEvent.values[0]
                accelY = sensorEvent.values[1]
                accelZ = sensorEvent.values[2]
            }
        }

        // 2. Pass combined current values to a single logic function
        processCombinedSensorData(proximityDistance, accelX, accelY, accelZ)
    }

    private fun processCombinedSensorData(
        proximityDistance: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float
    ) {
        // Simple logic: If phone is tilted up (y > 4) and screen is off
            if (accelY > 3.9 && proximityDistance > 0 /*&& !powerManager.isInteractive*/) {
                // This wakes the screen.
                // Once screen wakes, ACTION_SCREEN_ON fires and stops the sensor.
                if (!screenWakeLock.isHeld) {
                    screenWakeLock.acquire(1000)
                }
            }
        this.accelY = 0.0F  // reset the value after screen locking, otherwise it can turn on the screen again if proximityDistance > 0
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Controller Service",
                NotificationManager.IMPORTANCE_MIN // low importance makes the notification with no sounds
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
        unregisterReceiver(screenStateReceiver)
        stopLiftingDetection()
        ServiceTrackerRepository.setServiceRunning(false)   // Mark as stopped when the service dies
        super.onDestroy()
    }
}