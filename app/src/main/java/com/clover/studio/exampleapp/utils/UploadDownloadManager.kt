package com.clover.studio.exampleapp.utils

import android.app.Activity
import android.net.Uri
import android.util.Base64
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
    private var chunkCount = 0L

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
        mimeType: String,
        fileType: String,
        filePieces: Long,
        file: File,
        fileUploadListener: FileUploadListener
    ) {
        Timber.d("${file.length()}")

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
                    Tools.sha256HashFromUri(activity, fileUri),
                    fileType,
                    1
                )

                startUploadAPI(uploadFile, filePieces, fileUploadListener)

                Timber.d("$uploadFile")
                piece++
            }
        }
    }

    private suspend fun startUploadAPI(
        uploadFile: UploadFile,
        chunks: Long,
        fileUploadListener: FileUploadListener
    ) {
        try {
            repository.uploadFiles(uploadFile.chunkToJson())
            fileUploadListener.filePieceUploaded()
        } catch (ex: Exception) {
            Tools.checkError(ex)
            fileUploadListener.fileUploadError()
            chunkCount = 1
            return
        }
        chunkCount++

        if (chunkCount == chunks) {
            verifyFileUpload(uploadFile, fileUploadListener)
            chunkCount = 1
        }
    }

    private suspend fun verifyFileUpload(
        uploadFile: UploadFile,
        fileUploadListener: FileUploadListener
    ) {
        try {
            val filePath = repository.verifyFile(uploadFile.fileToJson()).data?.file?.path
            filePath?.let { fileUploadListener.fileUploadVerified(it) }
        } catch (ex: Exception) {
            Tools.checkError(ex)
            fileUploadListener.fileUploadError()
            return
        }
    }
}

interface FileUploadListener {
    fun filePieceUploaded()
    fun fileUploadError()
    fun fileUploadVerified(path: String)
}