package com.clover.studio.exampleapp.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.clover.studio.exampleapp.BuildConfig
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
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
            .getInstance("SHA-256")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    fun getHeaderMap(token: String?): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        if (!token.isNullOrEmpty()) {
            map[Const.Headers.ACCESS_TOKEN] = token
        }
        map[Const.Headers.OS_NAME] = Const.Headers.ANDROID
        map[Const.Headers.OS_VERSION] = Build.VERSION.SDK_INT.toString()
        map[Const.Headers.DEVICE_NAME] = Build.MODEL
        map[Const.Headers.APP_VERSION] = BuildConfig.VERSION_NAME
        map[Const.Headers.LANGUAGE] = Locale.getDefault().language

        return map
    }

    fun copyStreamToFile(activity: Activity, inputStream: InputStream): File {
        val outputFile = File(activity.cacheDir, "tempFile.jpeg")
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
        return outputFile
    }

    @Throws(IOException::class)
    fun createImageFile(activity: Activity?): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    fun convertBitmapToUri(activity: Activity, bitmap: Bitmap): Uri {
        val file = createImageFile(activity)

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0 /*ignored for PNG*/, bos);
        val bitmapdata = bos.toByteArray();

        val fos = FileOutputStream(file)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()

        return FileProvider.getUriForFile(
            activity,
            "com.clover.studio.exampleapp.fileprovider",
            file
        )
    }

    fun calculateSHA256FileHash(updateFile: File?): String? {
        val digest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            Timber.d("Exception while getting digest", e)
            return null
        }
        val inputStream: InputStream = try {
            FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            Timber.d("Exception while getting FileInputStream", e)
            return null
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            val sha256sum: ByteArray = digest.digest()
            val bigInt = BigInteger(1, sha256sum)
            var output: String = bigInt.toString(16)
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0')
            output
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for SHA-256", e)
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                Timber.d("Exception on closing SHA-256 input stream", e)
            }
        }
    }
}