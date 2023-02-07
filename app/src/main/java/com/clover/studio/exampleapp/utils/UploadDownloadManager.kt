@file:Suppress("BlockingMethodInNonBlockingContext")

package com.clover.studio.exampleapp.utils

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import com.clover.studio.exampleapp.data.models.FileMetadata
import com.clover.studio.exampleapp.data.models.UploadFile
import com.clover.studio.exampleapp.data.repositories.MainRepositoryImpl
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*


const val CHUNK_SIZE = 64000

/**
 * Methods below will handle all file upload logic for the app. The interface below will communicate
 * upload status to the subscribers. The caller will have to handle progress bar status by itself and
 * can update the status based on pieces sent or final file verification.
 */
class UploadDownloadManager constructor(
    private val repository: MainRepositoryImpl
) {
    private var chunkCount = 0

    /**
     * Method will handle file upload process. The caller will have to supply the required parameters
     *
     * @param activity The calling activity
     * @param fileUri The Uri path value of the file being uploaded
     * @param mimeType The mime type of the file (image, audio, video...)
     * @param fileType The type of the file being uploaded, in the context of the app. (avatar, message, group avatar...)
     * @param filePieces The number of the pieces the file has been divided to based on the maximum
     *  chunk size
     * @param file The file that is being uploaded to the backend
     * @param fileUploadListener The interface listener which will notify caller about update status.
     */
    suspend fun uploadFile(
        activity: Activity,
        fileUri: Uri,
        fileType: String,
        filePieces: Int,
        file: File,
        isThumbnail: Boolean = false,
        fileUploadListener: FileUploadListener
    ) {
        var fileMetadata: FileMetadata? = null
        val time: Int
        val width: Int
        val height: Int
        val mimeType = activity.contentResolver.getType(fileUri)!!

        // Check mime type of file being sent. If it is a media file get metadata for image or video
        // respectively
        if (mimeType.contains(Const.JsonFields.IMAGE_TYPE) || isThumbnail) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            height = options.outHeight
            width = options.outWidth

            fileMetadata = FileMetadata(width, height, null)
            Timber.d("File metadata: $fileMetadata")
        } else if (mimeType.contains(Const.JsonFields.VIDEO_TYPE)) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(activity, fileUri)
            time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                .toInt()
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt()
            height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                    .toInt()
            retriever.release()

            fileMetadata = FileMetadata(width, height, time)
            Timber.d("File metadata: $fileMetadata")
        }

        chunkCount = 0
        BufferedInputStream(FileInputStream(file)).use { bis ->
            var len: Int
            var piece = 0L
            val temp = ByteArray(CHUNK_SIZE)
            val randomId = UUID.randomUUID().toString().substring(0, 7)
            while (bis.read(temp).also { len = it } > 0) {
                val uploadFile = UploadFile(
                    Base64.encodeToString(
                        temp,
                        0,
                        len,
                        0
                    ),
                    piece,
                    filePieces,
                    file.length(),
                    mimeType,
                    file.name.toString(),
                    randomId,
                    Tools.sha256HashFromUri(
                        activity,
                        fileUri,
                        mimeType
                    ),
                    fileType,
                    fileMetadata
                )

                Timber.d("Chunk count $chunkCount")
                startUploadAPI(uploadFile, mimeType, filePieces, isThumbnail, fileUploadListener)

                piece++
            }
        }
    }

    private suspend fun startUploadAPI(
        uploadFile: UploadFile,
        mimeType: String,
        chunks: Int,
        isThumbnail: Boolean = false,
        fileUploadListener: FileUploadListener
    ) {
        try {
            repository.uploadFiles(uploadFile.chunkToJson())
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
            verifyFileUpload(uploadFile, mimeType, isThumbnail, fileUploadListener)
            chunkCount = 0
        }
    }

    private suspend fun verifyFileUpload(
        uploadFile: UploadFile,
        mimeType: String,
        isThumbnail: Boolean = false,
        fileUploadListener: FileUploadListener
    ) {
        try {
            val file = repository.verifyFile(uploadFile.fileToJson()).data?.file
            Timber.d("UploadDownload FilePath = ${file?.path}")
            Timber.d("Mime type = $mimeType")
            if (isThumbnail) file?.path?.let {
                fileUploadListener.fileUploadVerified(
                    it,
                    mimeType,
                    file.id.toLong(),
                    0
                )
            }
            else file?.path?.let {
                fileUploadListener.fileUploadVerified(
                    it,
                    mimeType,
                    0,
                    file.id.toLong()
                )
            }

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
    fun fileUploadVerified(path: String, mimeType: String, thumbId: Long = 0, fileId: Long = 0)
}