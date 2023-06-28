package com.clover.studio.spikamessenger.utils.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.JsonMessage
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UploadService : Service() {
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private var uploadJob: Job? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val items = intent?.getParcelableArrayListExtra<FileData>("files")

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "upload_channel"
            val channelName = "Upload Service Channel"
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "upload_channel")
            .setContentTitle("Uploading files")
            .setContentText("Uploading in progress...")
            .setSmallIcon(R.drawable.img_spika_push_black)
            .build()

        // Start the foreground service with the notification
        startForeground(Const.Service.UPLOAD_SERVICE_ID, notification)

        // Continue with the upload process
        uploadJob = CoroutineScope(Dispatchers.Default).launch {
            items?.toList()?.let { uploadItems(it) }
            stopForeground(STOP_FOREGROUND_REMOVE) // Stop the foreground service
            stopSelf() // Stop the service after all items are uploaded
        }

        return START_STICKY
    }

    override fun onDestroy() {
        uploadJob?.cancel()
        super.onDestroy()
    }

    private suspend fun uploadItems(items: List<FileData>) {
        for (item in items) {
            // Synchronous upload logic for each item
            // Implement your own upload logic here
            uploadItem(item)
        }
    }

    private suspend fun uploadItem(item: FileData) {
        // Implement your own upload logic here
        // For example, you can use Retrofit, OkHttp, or other libraries
        // to send the item to the web server
        // You can also handle the response from the server if needed
        uploadDownloadManager.uploadFile(item, object : FileUploadListener {
            override fun filePieceUploaded() {

            }

            override fun fileUploadError(description: String) {

            }

            override fun fileUploadVerified(
                path: String,
                mimeType: String,
                thumbId: Long,
                fileId: Long,
                fileType: String,
                messageBody: MessageBody?
            ) {
                if (!item.isThumbnail) {
                    if (fileId > 0) item.messageBody?.fileId =
                        fileId

//                    sendMessage(
//                        fileType,
//                        messageBody?.text!!,
//                        messageBody.fileId!!,
//                        messageBody.thumbId!!,
//                        1,
//                        item.localId,
//                    )
                }
            }
        })
    }

//    private fun sendMessage(
//        messageFileType: String,
//        text: String,
//        fileId: Long,
//        thumbId: Long,
//        roomId: Int,
//        localId: String,
//    ) {
//        val jsonMessage = JsonMessage(
//            text,
//            messageFileType,
//            fileId,
//            thumbId,
//            roomId,
//            localId,
//            null
//        )
//
//        val jsonObject = jsonMessage.messageToJson()
//        Timber.d("Message object: $jsonObject")
//        viewModel.sendMessage(jsonObject)
//    }
}
