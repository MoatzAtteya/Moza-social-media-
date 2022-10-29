package com.example.moza.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.moza.R
import com.example.moza.activities.HomeActivity
import com.example.moza.common.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*


private const val CHANNEL_ID = "my_channel"
private const val LIKE_CHANNEL_ID = "likes"
private const val COMMENT_CHANNEL_ID = "comments"


class FirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val intent = Intent(this, HomeActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Log.d("firebase notification:", "entered.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
            createLikesNotificationChannel(notificationManager)
            createCommentNotificationChannel(notificationManager)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        //val imgRL = message.data["ProfileImgURL"]
       // val url = URL(imgRL)
        //val img = BitmapFactory.decodeStream(url.openConnection().getInputStream())

        //val notificationID = message.data["notificationsID"]!!.toInt()
        val notificationID = Random().nextInt()
        println("notification data is: ${message.data}")

        if (message.data["notificationType"] == Constants.NOTIFICATION_MESSAGE_TYPE){
            if (!message.data["message"].isNullOrEmpty()){
                val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(message.data["userName"])
                    .setContentText(message.data["message"])
                    .setSmallIcon(R.drawable.moza_splashscreen)
                    //.setLargeIcon(img)
                    .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(notificationID,notification)
            }
        }else if (message.data["notificationType"] == Constants.NOTIFICATION_LIKE_TYPE){
            if (!message.data["message"].isNullOrEmpty()){
                val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(this, LIKE_CHANNEL_ID)
                    .setContentTitle(message.data["userName"])
                    .setContentText(message.data["message"])
                    .setSmallIcon(R.drawable.moza_splashscreen)
                    //.setLargeIcon(img)
                    .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(  notificationID,notification)

            }else if(message.data["notificationType"] == Constants.NOTIFICATION_COMMENT_TYPE){
                if (!message.data["message"].isNullOrEmpty()){
                    val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(this, COMMENT_CHANNEL_ID)
                        .setContentTitle(message.data["userName"])
                        .setContentText(message.data["message"])
                        .setSmallIcon(R.drawable.moza_splashscreen)
                        //.setLargeIcon(img)
                        .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build()
                    notificationManager.notify(  notificationID,notification)

                }
            }
        }





       /* val notificationSummary = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.moza_splashscreen)
            .setStyle(NotificationCompat.InboxStyle()
                .setBigContentTitle("New messages")
                .setSummaryText("Moza"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup("example_group")
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setGroupSummary(true)
            .build()*/

       // notificationManager.notify(notificationID,notificationSummary)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "messagesChannel"
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLikesNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "LikeChannel"
        val channel = NotificationChannel(
            LIKE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Like channel "
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCommentNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "commentChannel"
        val channel = NotificationChannel(
            COMMENT_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "comments channel "
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }


// TODO: send boolean with intent to notify the mainACtivity that it'c coming from notification then navigate to chat fragment.
}

