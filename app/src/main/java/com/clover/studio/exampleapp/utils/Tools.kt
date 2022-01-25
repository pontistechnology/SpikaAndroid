package com.clover.studio.exampleapp.utils

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.clover.studio.exampleapp.BuildConfig
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.security.MessageDigest
import java.util.*

object Tools {
    fun checkError(ex: Exception) {
        when (ex) {
            is IllegalArgumentException -> Timber.d("IllegalArgumentException")
            is IOException -> Timber.d("IOException")
            is HttpException -> Timber.d("HttpException: ${ex.code()} ${ex.message}")
            else -> Timber.d("UnknownError: ${ex.message}")
        }
    }

    fun getRandomImageUrl(randomNumber: Int): String {
        return when (randomNumber) {
            0 -> "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Owner_Mr_Paul_G_Jacobs.svg/1024px-Owner_Mr_Paul_G_Jacobs.svg.png"
            1 -> "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Aurthur_A_Collins.jpg/1280px-Aurthur_A_Collins.jpg"
            2 -> "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Algérie_-_Arménie_-_20140531_-_Mauro_Guevgeozian.jpg/1024px-Algérie_-_Arménie_-_20140531_-_Mauro_Guevgeozian.jpg"
            3 -> "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Iran_location_map.svg/1920px-Iran_location_map.svg.png"
            4 -> "https://upload.wikimedia.org/wikipedia/commons/d/db/Matudaira_Yoritoshi.jpg"
            else -> "https://upload.wikimedia.org/wikipedia/commons/d/db/Matudaira_Yoritoshi.jpg"
        }
    }

    fun formatE164Number(context: Context, countryCode: String?, phNum: String?): String? {
        val e164Number: String? = if (TextUtils.isEmpty(countryCode)) {
            phNum
        } else {

            val telephonyManager =
                ContextCompat.getSystemService(context, TelephonyManager::class.java)
            val isoCode = telephonyManager?.simCountryIso

            Timber.d("Country code: ${isoCode?.uppercase()}")
            PhoneNumberUtils.formatNumberToE164(phNum, isoCode?.uppercase())
        }
        return e164Number
    }

    fun hashString(input: String): String {
        val hexChars = "0123456789abcdef"
        val bytes = MessageDigest
            .getInstance("SHA-1")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    fun getHeaderMap(token: String): Map<String, String> {
        return mutableMapOf(
            Const.Headers.ACCESS_TOKEN to token,
            Const.Headers.OS_NAME to Const.Headers.ANDROID,
            Const.Headers.OS_VERSION to Build.VERSION.SDK_INT.toString(),
            Const.Headers.DEVICE_NAME to Build.MODEL,
            Const.Headers.APP_VERSION to BuildConfig.VERSION_NAME,
            Const.Headers.LANGUAGE to Locale.getDefault().language
        )
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