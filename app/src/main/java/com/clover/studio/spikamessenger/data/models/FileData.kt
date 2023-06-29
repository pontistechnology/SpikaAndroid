package com.clover.studio.spikamessenger.data.models

import android.net.Uri
import android.os.Parcelable
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class FileData(
    val fileUri: Uri,
    val fileType: String,
    val filePieces: Int,
    val file: File,
    val messageBody: MessageBody?,
    val isThumbnail: Boolean = false,
    val localId: String?,
    val roomId: Int
) : Parcelable
