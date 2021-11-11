package com.clover.studio.exampleapp.utils

import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

object Tools {
    fun checkError(ex: Exception) {
        when (ex) {
            is IllegalArgumentException -> Timber.d("IllegalArgumentException")
            is IOException -> Timber.d("IOException")
            is HttpException -> Timber.d("HttpException: ${ex.code()} ${ex.message}")
            else -> Timber.d("UnknownError: ${ex.message}")
        }
    }

    // TODO make temporary file Uri for camera picture taken
//    fun makeTempFile(activity: Activity): File? {
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        val imageFileName = "JPEG_" + timeStamp + "_"
//        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        var photoFile: File? = null
//        try {
//            photoFile = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",  /* suffix */
//                storageDir /* directory */
//            )
//        } catch (e: IOException) {
//            e.printStackTrace()
//            Timber.d("IOException")
//        }
//        return photoFile
//    }
}