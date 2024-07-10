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
import com.clover.studio.spikamessenger.data.repositories.ChatRepository
import com.clover.studio.spikamessenger.data.repositories.MainRepository
import com.clover.studio.spikamessenger.utils.CHANNEL_ID
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UploadService : Service() {
    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var ChatRepository: ChatRepository

    private var uploadJob: Job? = null
    private var localIdMap: MutableMap<String, Long> = mutableMapOf()
    private var uploadedFiles: MutableList<FileData> = mutableListOf()

    private val binder = UploadServiceBinder()
    private var callbackListener: FileUploadCallback? = null
    private var count = 0

    private var uploadCounterThumbnail = 0
    private var uploadCounterImage = 0

    private var thumbnailJobs: List<Job> = mutableListOf()
    private var imageJobs: List<Job> = mutableListOf()
    private var filesWaiting: MutableList<FileData> = mutableListOf()

    private var sharedFiles: MutableList<JsonObject> = mutableListOf()

    private val jobMap: MutableMap<String?, Job> = mutableMapOf()

    private var isSharing: Boolean = false

    inner class UploadServiceBinder : Binder() {
        fun getService(): UploadService = this@UploadService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setCallbackListener(listener: FileUploadCallback) {
        this.callbackListener = listener
    }

    fun removeCallbackListener() {
        this.callbackListener = null
    }

    private fun updateProgress(progress: Int, maxProgress: Int, localId: String) {
        callbackListener?.updateUploadProgressBar(progress, maxProgress, localId)
    }

    private fun uploadingFinished(uploadedFiles: MutableList<FileData>) {
        callbackListener?.uploadingFinished(uploadedFiles, sharedFiles)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.uploading_files))
            .setContentText(getString(R.string.upload_in_progress_notification))
            .setSmallIcon(R.drawable.spika_base_logo)
            .build()

        // Start the foreground service with the notification
        startForeground(Const.Service.UPLOAD_SERVICE_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        uploadJob?.cancel()
        super.onDestroy()
    }

    suspend fun uploadAvatar(fileData: FileData, isGroup: Boolean) {
        UploadDownloadManager(mainRepository).uploadFile(
            fileData,
            object : FileUploadListener {
                override fun filePieceUploaded() {
                    // avatar upload doesn't use progress
                }

                override fun fileUploadError(description: String) {
                    callbackListener?.uploadError(description)
                    CoroutineScope(Dispatchers.IO).launch { resetUpload() }
                }

                override fun fileUploadVerified(
                    path: String,
                    mimeType: String,
                    thumbId: Long,
                    fileId: Long,
                    fileType: String,
                    messageBody: MessageBody?
                ) {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty(Const.UserData.AVATAR_FILE_ID, fileId)

                    CoroutineScope(Dispatchers.Default).launch {
                        if (isGroup) {
                            jsonObject.addProperty(
                                Const.JsonFields.ACTION,
                                Const.JsonFields.CHANGE_GROUP_AVATAR
                            )
                            mainRepository.updateRoom(
                                jsonObject,
                                fileData.roomId
                            )
                        } else {
                            mainRepository.updateUserData(jsonObject)
                        }
                    }
                    callbackListener?.avatarUploadFinished(fileId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return
                }

                override fun fileCanceledListener(messageId: String?) {
                    // avatar upload cannot be cancelled
                }
            })
    }

    suspend fun uploadItems(items: List<FileData>, isSharing: Boolean = false) {
        this.isSharing = isSharing
        if (thumbnailJobs.isNotEmpty() || imageJobs.isNotEmpty()) {
            filesWaiting.addAll(items)
            return
        }

        uploadedFiles.addAll(items)
        coroutineScope {
            thumbnailJobs = items.filter { it.isThumbnail }.map { item ->
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

            imageJobs = items.filter { !it.isThumbnail }.map { item ->
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

    private suspend fun resetUpload() {
        jobMap.clear()
        thumbnailJobs = mutableListOf()
        imageJobs = mutableListOf()

        if (filesWaiting.isNotEmpty()) {
            val filesToUpload: MutableList<FileData> = mutableListOf()
            filesToUpload.addAll(filesWaiting)
            filesWaiting.clear()
            uploadItems(filesToUpload)
            return
        }

        isSharing = false
        uploadingFinished(uploadedFiles)
        uploadedFiles.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun uploadItem(item: FileData) {
        UploadDownloadManager(mainRepository).uploadFile(item, object : FileUploadListener {
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
                        isSharing = isSharing
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
        isSharing: Boolean
    ) {
        val jsonMessage = JsonMessage(
            msgText = text,
            mimeType = messageFileType,
            fileId = fileId,
            thumbId = thumbId,
            roomId = roomId,
            localId = localId,
            replyId = null
        )

        val jsonObject = jsonMessage.messageToJson()
        if (isSharing) {
            sharedFiles.add(jsonObject)
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                ChatRepository.sendMessage(jsonObject)
            }
        }
    }

    interface FileUploadCallback {
        fun updateUploadProgressBar(progress: Int, maxProgress: Int, localId: String?) {}
        fun uploadingFinished(
            uploadedFiles: MutableList<FileData>,
            sharedFiles: MutableList<JsonObject>
        ) {
        }

        fun uploadError(description: String) {}
        fun avatarUploadFinished(fileId: Long) {}
    }
}
