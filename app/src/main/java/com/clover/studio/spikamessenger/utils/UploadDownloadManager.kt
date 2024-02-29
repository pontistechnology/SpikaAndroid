@file:Suppress("BlockingMethodInNonBlockingContext")

package com.clover.studio.spikamessenger.utils

import android.util.Base64
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.FileMetadata
import com.clover.studio.spikamessenger.data.models.UploadFile
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.repositories.MainRepositoryImpl
import com.clover.studio.spikamessenger.utils.helpers.Resource
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.*


fun getChunkSize(fileSize: Long): Int = if (fileSize > ONE_GB) ONE_MB * 2 else ONE_MB

const val ONE_MB = 1024 * 1024
const val ONE_GB = 1024 * 1024 * 1024

/**
 * Methods below will handle all file upload logic for the app. The interface below will communicate
 * upload status to the subscribers. The caller will have to handle progress bar status by itself and
 * can update the status based on pieces sent or final file verification.
 */
class UploadDownloadManager constructor(
    private val repository: MainRepositoryImpl
) {
    private var chunkCount = 0
    private var cancelUpload = false

    /**
     * Method will handle file upload process. The caller will have to supply the required parameters
     *  chunk size
     *  @param fileData
     * @param fileUploadListener The interface listener which will notify caller about update status.
     */
    suspend fun uploadFile(
        fileData: FileData,
        fileUploadListener: FileUploadListener,
    ) {
        var mimeType = MainApplication.appContext.contentResolver.getType(fileData.fileUri)
            ?.takeIf { it.isNotEmpty() }
            ?: if (fileData.fileUri.toString().contains(Const.JsonFields.GIF)) Const.JsonFields.IMAGE_TYPE else ""

        Timber.d("MIME TYPE:::: $mimeType")

        cancelUpload = false

        if (Tools.forbiddenMimeTypes(mimeType)) {
            mimeType = Const.JsonFields.FILE_TYPE
        }

        val fileMetadata: FileMetadata? =
            Tools.getMetadata(fileData.fileUri, mimeType, fileData.isThumbnail)

        chunkCount = 0
        cancelUpload = false
        BufferedInputStream(FileInputStream(fileData.file)).use { bis ->
            var len: Int
            var piece = 0L
            val temp = ByteArray(getChunkSize(fileData.file.length()))
            val randomId = UUID.randomUUID().toString().substring(0, 7)

            while ((bis.read(temp).also { len = it } > 0) && !cancelUpload) {
                val uploadFile = UploadFile(
                    chunk = Base64.encodeToString(
                        temp,
                        0,
                        len,
                        0
                    ),
                    offset = piece,
                    total = fileData.filePieces,
                    size = fileData.file.length(),
                    mimeType = mimeType,
                    fileName = fileData.file.name.toString(),
                    clientId = randomId,
                    fileHash = Tools.sha256HashFromUri(
                        fileData.fileUri,
                        mimeType
                    ),
                    type = fileData.fileType,
                    metaData = fileMetadata
                )

                Timber.d("Chunk count $chunkCount")
                startUploadAPI(
                    uploadFile = uploadFile,
                    mimeType = mimeType,
                    chunks = fileData.filePieces,
                    isThumbnail = fileData.isThumbnail,
                    messageBody = fileData.messageBody,
                    fileUploadListener = fileUploadListener,
                )
                piece++
            }
        }
    }

    private suspend fun startUploadAPI(
        uploadFile: UploadFile,
        mimeType: String,
        chunks: Int,
        isThumbnail: Boolean = false,
        messageBody: MessageBody?,
        fileUploadListener: FileUploadListener,
    ) {
        try {
            val response = repository.uploadFiles(uploadFile.chunkToJson())
            if (Resource.Status.CANCEL == response.status) {
                fileUploadListener.fileCanceledListener(response.message)
                cancelUpload = true
                return
            }
            fileUploadListener.filePieceUploaded()
        } catch (ex: Exception) {
            Tools.checkError(ex)
            fileUploadListener.fileUploadError(ex.message.toString())
            chunkCount = 0
            return
        }
        chunkCount++

        Timber.d("Should verify? $chunkCount, $chunks")
        if (chunkCount == chunks) {
            verifyFileUpload(uploadFile, mimeType, isThumbnail, messageBody, fileUploadListener)
            chunkCount = 0
        }
    }

    private suspend fun verifyFileUpload(
        uploadFile: UploadFile,
        mimeType: String,
        isThumbnail: Boolean = false,
        messageBody: MessageBody?,
        fileUploadListener: FileUploadListener
    ) {
        try {
            val file = repository.verifyFile(uploadFile.fileToJson()).responseData?.data?.file
            Timber.d("UploadDownload FilePath = ${file?.path}")
            Timber.d("Mime type = $mimeType")
            if (file != null) {
                if (isThumbnail) file.path?.let {
                    fileUploadListener.fileUploadVerified(
                        path = it,
                        mimeType = mimeType,
                        thumbId = file.id.toLong(),
                        fileId = 0,
                        fileType = file.type!!,
                        messageBody = messageBody
                    )
                }
                else file.path?.let {
                    fileUploadListener.fileUploadVerified(
                        path = it,
                        mimeType = mimeType,
                        thumbId = 0,
                        fileId = file.id.toLong(),
                        fileType = file.type!!,
                        messageBody = messageBody
                    )
                }
            } else fileUploadListener.fileUploadError("Some error")

        } catch (ex: Exception) {
            Tools.checkError(ex)
            fileUploadListener.fileUploadError(ex.message.toString())
            return
        }
    }
}

interface FileUploadListener {
    fun filePieceUploaded()
    fun fileUploadError(description: String)
    fun fileUploadVerified(
        path: String,
        mimeType: String,
        thumbId: Long = 0,
        fileId: Long = 0,
        fileType: String,
        messageBody: MessageBody?
    )
    fun fileCanceledListener(
        messageId: String?,
    )
}
