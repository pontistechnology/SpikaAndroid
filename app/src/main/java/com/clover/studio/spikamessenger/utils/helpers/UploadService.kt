package com.clover.studio.spikamessenger.utils.helpers

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.JsonMessage
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.MainRepositoryImpl
import com.clover.studio.spikamessenger.utils.CHANNEL_ID
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UploadService : Service() {
    @Inject
    lateinit var mainRepositoryImpl: MainRepositoryImpl

    @Inject
    lateinit var chatRepositoryImpl: ChatRepositoryImpl

    private var uploadJob: Job? = null
    private var localIdMap: MutableMap<String, Long> = mutableMapOf()
    private var uploadedFiles: MutableList<FileData> = mutableListOf()

    private val binder = UploadServiceBinder()
    private var callbackListener: FileUploadCallback? = null
    private var count = 0

    private var uploadCounterThumbnail = 0
    private var uploadCounterImage = 0

    private val jobMap: MutableMap<String?, Job> = mutableMapOf()

    inner class UploadServiceBinder : Binder() {
        fun getService(): UploadService = this@UploadService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setCallbackListener(listener: FileUploadCallback) {
        callbackListener = listener
    }

    private fun updateProgress(progress: Int, maxProgress: Int, localId: String) {
        callbackListener?.updateUploadProgressBar(progress, maxProgress, localId)
    }

    private fun uploadingFinished(uploadedFiles: MutableList<FileData>) {
        callbackListener?.uploadingFinished(uploadedFiles)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val items = intent?.getParcelableArrayListExtra<FileData>(Const.IntentExtras.FILES_EXTRA)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.uploading_files))
            .setContentText(getString(R.string.upload_in_progress_notification))
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
        uploadedFiles.addAll(items)
        coroutineScope {
            val thumbnailJobs = items.filter { it.isThumbnail }.map { item ->
                delay(500)
                launch {
                    uploadItem(item)
                    uploadCounterThumbnail++
                }.also { jobMap[item.localId] = it }
            }

            thumbnailJobs.joinAll()

            if (thumbnailJobs.all { it.isCompleted }) {
                jobMap.clear()
                thumbnailJobs.forEach {
                    it.cancel()
                }
            }

            val imageJobs = items.filter { !it.isThumbnail }.map { item ->
                delay(500)
                launch {
                    uploadItem(item)
                    uploadCounterImage++
                }.also { jobMap[item.localId] = it }
            }

            imageJobs.joinAll()
            imageJobs.forEach {
                it.cancel()
            }
            if (imageJobs.all { it.isCompleted }) {
                resetUpload()
            }
        }
    }

    private fun resetUpload() {
        jobMap.clear()
        uploadingFinished(uploadedFiles)
        uploadedFiles.clear()
    }

    private suspend fun uploadItem(item: FileData) {
        UploadDownloadManager(mainRepositoryImpl).uploadFile(item, object : FileUploadListener {
            override fun filePieceUploaded() {
                if (!item.isThumbnail) {
                    count += 1
                    updateProgress(count, item.filePieces, item.localId.toString())
                }
            }

            override fun fileUploadError(description: String) {
                uploadedFiles.forEach {
                    if (it.localId == item.localId) {
                        it.messageStatus = Resource.Status.ERROR
                    }
                }
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
                    var fileThumbId: Long? = null

                    if (fileId > 0) messageBody?.fileId =
                        fileId

                    if (localIdMap[item.localId] != 0L) {
                        fileThumbId = localIdMap[item.localId]
                    }

                    sendMessage(
                        messageFileType = fileType,
                        text = messageBody?.text!!,
                        fileId = messageBody.fileId!!,
                        thumbId = fileThumbId ?: messageBody.thumbId!!,
                        roomId = item.roomId,
                        localId = item.localId!!,
                    )
                    uploadedFiles.find { it.localId == item.localId && !it.isThumbnail }?.messageStatus =
                        Resource.Status.SUCCESS
                } else {
                    item.localId?.let { localIdMap.put(it, thumbId) }
                    uploadedFiles.find { it.localId == item.localId && it.isThumbnail }?.messageStatus =
                        Resource.Status.SUCCESS
                }
            }

            override fun fileCanceledListener(messageId: String?) {
                if (messageId != null) {
                    jobMap[messageId]?.cancel()
                    jobMap.remove(messageId)
                }
            }
        })
    }

    private fun sendMessage(
        messageFileType: String,
        text: String,
        fileId: Long,
        thumbId: Long,
        roomId: Int,
        localId: String,
    ) {
        val jsonMessage = JsonMessage(
            text,
            messageFileType,
            fileId,
            thumbId,
            roomId,
            localId,
            null
        )

        val jsonObject = jsonMessage.messageToJson()
        Timber.d("Message object: $jsonObject")
        CoroutineScope(Dispatchers.Default).launch {
            chatRepositoryImpl.sendMessage(jsonObject)
        }
    }

    interface FileUploadCallback {
        fun updateUploadProgressBar(progress: Int, maxProgress: Int, localId: String?)
        fun uploadingFinished(uploadedFiles: MutableList<FileData>)
    }
}
