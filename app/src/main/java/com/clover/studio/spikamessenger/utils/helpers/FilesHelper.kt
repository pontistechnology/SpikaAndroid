package com.clover.studio.spikamessenger.utils.helpers

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.FileMetadata
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.MessageFile
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.getChunkSize
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object FilesHelper {

    fun uploadFile(
        isThumbnail: Boolean,
        uri: Uri,
        localId: String,
        roomId: Int,
        metadata: FileMetadata?
    ): FileData {

        val messageBody = MessageBody(
            referenceMessage = null,
            text = "",
            fileId = 0,
            thumbId = 0,
            file = null,
            thumb = null,
            subjectId = null,
            objectIds = null,
            type = "",
            objects = null,
            subject = ""
        )
        val inputStream =
            MainApplication.appContext.contentResolver.openInputStream(uri)

        val fileName = Tools.getFileNameFromUri(uri)
        val fileStream = Tools.copyStreamToFile(
            inputStream = inputStream!!,
            extension = Tools.getFileMimeType(MainApplication.appContext, uri)!!,
            fileName = fileName
        )
        val uploadPieces =
            if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
            else (fileStream.length() / getChunkSize(fileStream.length())).toInt()

        val fileType = Tools.getFileType(uri)

        inputStream.close()

        return FileData(
            fileUri = uri,
            fileType = fileType,
            filePieces = uploadPieces,
            file = fileStream,
            messageBody = messageBody,
            isThumbnail = isThumbnail,
            localId = localId,
            roomId = roomId,
            messageStatus = null,
            metadata = metadata,
        )
    }

    fun createTempFile(
        uri: Uri,
        type: String,
        localUserId: Int,
        roomId: Int,
        unsentMessages: MutableList<Message>
    ): Message {
        val fileName = Tools.getFileNameFromUri(uri)
        var size = 0L

        if (type == Const.JsonFields.FILE_TYPE) {
            val inputStream =
                MainApplication.appContext.contentResolver.openInputStream(uri)
            size = Tools.copyStreamToFile(
                inputStream!!,
                MainApplication.appContext.contentResolver.getType(uri)!!,
                fileName
            ).length()
            inputStream.close()
        }

        val typeMedia =
            if (MainApplication.appContext.contentResolver.getType(uri) == Const.FileExtensions.AUDIO)
                Const.JsonFields.AUDIO_TYPE
            else
                type

        val fileMetadata: FileMetadata? =
            Tools.getMetadata(uri, type, true)
        Timber.d("File metadata: $fileMetadata")

        val tempMessage = Tools.createTemporaryMessage(
            id = getUniqueRandomId(unsentMessages),
            localUserId = localUserId,
            roomId = roomId,
            messageType = typeMedia,
            messageBody = MessageBody(
                referenceMessage = null,
                text = null,
                fileId = 1,
                thumbId = 1,
                file = MessageFile(
                    id = 1,
                    fileName = fileName,
                    mimeType = "",
                    size = size,
                    metaData = fileMetadata,
                    uri = uri.toString()
                ),
                thumb = null,
                subjectId = null,
                objectIds = null,
                type = "",
                objects = null,
                subject = ""
            )
        )

        if (typeMedia == Const.JsonFields.IMAGE_TYPE || typeMedia == Const.JsonFields.VIDEO_TYPE) {
            saveMediaToStorage(
                MainApplication.appContext,
                MainApplication.appContext.contentResolver,
                uri,
                tempMessage.localId
            )
        }

        return tempMessage
    }

    /**
     * Method creates a random Integer from its lowest value to 0. This is to ensure that the
     * message in question is a temporary message, since all real messages have positive values.
     * After creating a random value, the method will check the current list containing unsent
     * messages and see if any of the items currently have the random number designated. This
     * ensures that no two unsent messages have the same id value. If it finds the same value
     * in the list, it will generate a new value and goi through the check again.
     **/
    fun getUniqueRandomId(unsentMessages: MutableList<Message>): Int {
        var randomId = Tools.generateRandomInt()
        while (unsentMessages.any { randomId == it.id }) {
            randomId = Tools.generateRandomInt()
        }
        return randomId
    }

    private fun saveMediaToStorage(
        context: Context,
        contentResolver: ContentResolver,
        mediaUri: Uri,
        id: String?
    ): String? {
        val inputStream = contentResolver.openInputStream(mediaUri)
        var outputStream: OutputStream? = null
        var imagePath: String? = null

        try {
            val tempFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "$id.${Const.FileExtensions.JPG}"
            )
            outputStream = FileOutputStream(tempFile)

            // Simple compression
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            //inputStream?.copyTo(outputStream)
            imagePath = tempFile.absolutePath


        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return imagePath
    }

}
