package com.rescuemate.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rescuemate.MainActivity
import com.rescuemate.R
import com.rescuemate.data.repository.FCMRepository
import com.rescuemate.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmergencyNotificationService : FirebaseMessagingService() {

    private val TAG = "EmergencyNotificationService"
    private val fcmRepository = FCMRepository(applicationContext)

    companion object {
        private const val CHANNEL_ID = "emergency_notifications"
        private const val CHANNEL_NAME = "Emergency Alerts"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}...")
        
        CoroutineScope(Dispatchers.IO).launch {
            fcmRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Has data payload: ${remoteMessage.data.isNotEmpty()}")
        Log.d(TAG, "Has notification payload: ${remoteMessage.notification != null}")
        
        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val messageType = remoteMessage.data["type"]
            Log.d(TAG, "Message type: $messageType")
            
            if (messageType == "EMERGENCY_ALERT") {
                Log.d(TAG, "Processing emergency alert notification")
                handleEmergencyNotification(remoteMessage)
            } else {
                Log.w(TAG, "Unknown message type: $messageType")
            }
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification payload - title: ${it.title}, body: ${it.body}")
        }
    }

    private fun handleEmergencyNotification(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val emergencyId = data["emergencyId"] ?: return
        val userId = data["userId"] ?: ""
        val userName = data["userName"] ?: "User"
        val emergencyType = data["emergencyType"] ?: "Emergency"
        val alertReason = data["alertReason"] ?: ""
        val location = data["location"] ?: ""
        val timestamp = data["timestamp"] ?: ""

        val notification = remoteMessage.notification
        
        val title = notification?.title ?: "Emergency Alert - $userName"
        val body = notification?.body ?: "$userName has initiated an SOS Protocol. Tap to view details."

        // Create intent to open EmergencyNotificationScreen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", Screen.EmergencyNotification.route)
            putExtra("emergencyId", emergencyId)
            putExtra("userId", userId)
            putExtra("userName", userName)
            putExtra("emergencyType", emergencyType)
            putExtra("alertReason", alertReason)
            putExtra("location", location)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency alerts and SOS protocol notifications"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
