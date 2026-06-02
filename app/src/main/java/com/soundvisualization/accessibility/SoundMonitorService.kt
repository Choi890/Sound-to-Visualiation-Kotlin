package com.soundvisualization.accessibility

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class SoundMonitorService : Service() {
    private var detector: SoundDetectionController? = null
    private var nameRecognizer: NameCallRecognizer? = null
    private var currentSettings = AlertSettings()
    private var currentWatchedName = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        detector = SoundDetectionController(
            context = applicationContext,
            onState = {},
            onEvent = { event -> handleDetectedEvent(event) },
        )
        nameRecognizer = NameCallRecognizer(
            context = applicationContext,
            watchedName = {
                if (currentSettings.categorySetting(AlertKind.NameCall).enabled) {
                    currentWatchedName
                } else {
                    ""
                }
            },
            speechLanguageTag = { preferredNameCallSpeechLanguageTag(currentWatchedName) },
            onPartialTranscript = { _, _ -> },
            onFinalTranscript = { _, _ -> },
            onDetected = { event -> handleDetectedEvent(event) },
            onAvailabilityChanged = {},
            onListeningChanged = {},
            onLevelChanged = {},
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val settings = LocalAppStore.loadAlertSettings(applicationContext).copy(alwaysOnEnabled = true)
        currentSettings = settings
        currentWatchedName = LocalAppStore.loadWatchedName(applicationContext)
        LocalAppStore.saveAlertSettings(applicationContext, settings)

        startForeground(
            NOTIFICATION_ID,
            LocalAppStore.loadAppLanguage(applicationContext).strings.let { strings ->
            buildMonitorNotification(
                title = strings.monitorTitle,
                message = strings.monitorMessage,
                strings = strings,
            )
            },
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        detector?.updateSettings(settings)
        updateNameCallSharedDetection(settings)
        detector?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        detector?.setSharedSpeechAudioSink(null)
        nameRecognizer?.close()
        nameRecognizer = null
        detector?.close()
        detector = null

        val settings = LocalAppStore.loadAlertSettings(applicationContext).copy(alwaysOnEnabled = false)
        LocalAppStore.saveAlertSettings(applicationContext, settings)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNameCallSharedDetection(settings: AlertSettings) {
        val recognizer = nameRecognizer ?: return
        val watchedName = currentWatchedName
        val shouldListenForNames =
            settings.categorySetting(AlertKind.NameCall).enabled &&
                parseWatchedNames(watchedName).isNotEmpty()

        if (!shouldListenForNames) {
            detector?.setSharedSpeechAudioSink(null)
            recognizer.stop(notify = false)
            return
        }

        recognizer.stop(notify = false)
        val started = recognizer.startSharedDetection()
        detector?.setSharedSpeechAudioSink(
            if (started) recognizer::acceptSharedAudio else null,
        )
    }

    private fun handleDetectedEvent(event: DetectedSoundEvent) {
        if (event.kind == AlertKind.Idle) return

        LocalAppStore.appendEvent(applicationContext, event)
        val settings = LocalAppStore.loadAlertSettings(applicationContext)
        currentSettings = settings
        currentWatchedName = LocalAppStore.loadWatchedName(applicationContext)
        if (settings.vibrationEnabled) {
            vibrateFor(event.kind)
        }

        val manager = getSystemService(NotificationManager::class.java)
        val strings = LocalAppStore.loadAppLanguage(applicationContext).strings
        manager?.notify(
            NOTIFICATION_ID,
            buildMonitorNotification(
                title = strings.monitorTitle,
                message = "${strings.recentDetectionPrefix}: ${strings.alertTitle(event.kind)} - ${strings.confidenceLabel(event.confidence)}",
                strings = strings,
            ),
        )

        if (event.kind in CRITICAL_NOTIFICATION_KINDS) {
            manager?.notify(
                ALERT_NOTIFICATION_BASE_ID + event.kind.ordinal,
                buildAlertNotification(event, strings),
            )
        }
    }

    private fun buildMonitorNotification(
        title: String,
        message: String,
        strings: AppStrings,
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SoundMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, strings.stopAlwaysOn, stopIntent)
            .build()
    }

    private fun buildAlertNotification(event: DetectedSoundEvent, strings: AppStrings): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            event.kind.ordinal + 10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val message = "${strings.severityLabel(event.severity)} - ${strings.confidenceLabel(event.confidence)}"

        return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${strings.alertTitle(event.kind)} ${strings.detectedSuffix}")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n${formatNotificationTime(event.id)}"),
            )
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(vibrationPattern(event.kind))
            .build()
    }

    private fun createNotificationChannel() {
        val strings = LocalAppStore.loadAppLanguage(applicationContext).strings

        val monitorChannel = NotificationChannel(
            CHANNEL_ID,
            strings.monitorChannelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = strings.monitorChannelDescription
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            strings.alertChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = strings.alertChannelDescription
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 70, 180, 70, 260)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        getSystemService(NotificationManager::class.java)?.createNotificationChannels(
            listOf(monitorChannel, alertChannel),
        )
    }

    private fun vibrateFor(kind: AlertKind) {
        val pattern = vibrationPattern(kind)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun vibrationPattern(kind: AlertKind): LongArray = when (kind.defaultSeverity()) {
        AlertSeverity.Emergency -> longArrayOf(0, 180, 70, 180, 70, 260)
        AlertSeverity.Caution -> longArrayOf(0, 120, 80, 180)
        AlertSeverity.Info -> longArrayOf(0, 70, 55, 70)
    }

    private fun formatNotificationTime(timeMillis: Long): String =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
            .format(java.util.Date(timeMillis))

    companion object {
        private const val CHANNEL_ID = "sound_monitor"
        private const val ALERT_CHANNEL_ID = "critical_sound_alerts"
        private const val NOTIFICATION_ID = 4802
        private const val ALERT_NOTIFICATION_BASE_ID = 4900
        private const val ACTION_STOP = "com.soundvisualization.accessibility.STOP_SOUND_MONITOR"
        private val CRITICAL_NOTIFICATION_KINDS = setOf(
            AlertKind.Siren,
            AlertKind.FireAlarm,
            AlertKind.Doorbell,
        )

        fun start(context: Context) {
            val intent = Intent(context, SoundMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SoundMonitorService::class.java))
        }
    }
}
