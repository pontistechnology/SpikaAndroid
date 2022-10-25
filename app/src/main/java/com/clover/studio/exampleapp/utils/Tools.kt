package com.clover.studio.exampleapp.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.R
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


const val BITMAP_WIDTH = 512
const val BITMAP_HEIGHT = 512
const val TO_MEGABYTE = 1000000
const val DEVICE_ID_LENGTH = 13
const val TOKEN_EXPIRED_CODE = 401

object Tools {
    var fileName: String = ""
    var pictureHeight: Int = 0
    var videoHeight: Int = 0

    fun checkError(ex: Exception): Boolean {
        when (ex) {
            is IllegalArgumentException -> Timber.d("IllegalArgumentException ${ex.message}")
            is IOException -> Timber.d("IOException ${ex.message}")
            is HttpException ->
                if (ex.code() == TOKEN_EXPIRED_CODE) {
                    Timber.d("Token Expired: ${ex.code()} ${ex.message}")
                    return true
                } else {
                    Timber.d("HttpException: ${ex.code()} ${ex.message}")
                }
            else -> Timber.d("UnknownError: ${ex.message}")
        }
        return false
    }

    fun formatE164Number(context: Context, countryCode: String?, phNum: String?): String? {
        val e164Number: String? = if (TextUtils.isEmpty(countryCode)) {
            phNum
        } else {

            val telephonyManager =
                getSystemService(context, TelephonyManager::class.java)
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

    fun copyStreamToFile(activity: Activity, inputStream: InputStream, extension: String): File {
        if (fileName.isEmpty()) {
            fileName = "tempFile${System.currentTimeMillis()}.${extension.substringAfterLast("/")}"
        }
        val outputFile = File(activity.cacheDir, fileName)
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

    fun calculateToMegabyte(size: Long): String? {
        val df = DecimalFormat("#.##")
        return df.format(size.toFloat().div(TO_MEGABYTE))
    }

    @Throws(IOException::class)
    fun createImageFile(activity: Activity?): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    fun convertBitmapToUri(activity: Activity, bitmap: Bitmap): Uri {
        val file = createImageFile(activity)

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos)
        val bitmapData = bos.toByteArray()

        val fos = FileOutputStream(file)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        return FileProvider.getUriForFile(
            activity,
            "com.clover.studio.exampleapp.fileprovider",
            file
        )
    }

    private fun calculateSHA256FileHash(updateFile: File?): String? {
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

    @Throws(IOException::class)
    fun handleSamplingAndRotationBitmap(context: Context, selectedImage: Uri?): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var imageStream = context.contentResolver.openInputStream(selectedImage!!)
        BitmapFactory.decodeStream(imageStream, null, options)
        imageStream?.close()

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        imageStream = context.contentResolver.openInputStream(selectedImage)
        var img = BitmapFactory.decodeStream(imageStream, null, options)
        img = img?.let { rotateImageIfRequired(context, it, selectedImage) }
        return img
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > BITMAP_HEIGHT || width > BITMAP_WIDTH) {

            // Calculate ratios of height and width to requested height and width
            val heightRatio = (height.toFloat() / BITMAP_HEIGHT.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / BITMAP_WIDTH.toFloat()).roundToInt()

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).
            val totalPixels = (width * height).toFloat()

            // Anything more than 2x the requested pixels we'll sample down further
            val totalReqPixelsCap = (BITMAP_WIDTH * BITMAP_HEIGHT * 2).toFloat()
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }

    @Throws(IOException::class)
    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap? {
        val input = context.contentResolver.openInputStream(selectedImage)
        val ei =
            input?.let { ExifInterface(it) }
        return when (ei?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    fun sha256HashFromUri(
        activity: Activity,
        currentPhotoLocation: Uri,
        extension: String
    ): String? {
        val sha256FileHash: String?
        val inputStream =
            activity.contentResolver.openInputStream(currentPhotoLocation)
        sha256FileHash =
            calculateSHA256FileHash(copyStreamToFile(activity, inputStream!!, extension))

        return sha256FileHash
    }

    fun getFileUrl(url: String): String {
        return when {
            url.startsWith(BuildConfig.SERVER_URL) -> url
            url.startsWith("/") -> BuildConfig.SERVER_URL + url.substring(1)
            else -> BuildConfig.SERVER_URL + "/" + url
        }
    }


    fun getRelativeTimeSpan(startDate: Long): CharSequence? {
        return DateUtils.getRelativeTimeSpanString(
            startDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
    }

    fun hideKeyboard(activity: Activity, view: View) {
        val inputMethodManager: InputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun generateDeviceId(): String {
        return UUID.randomUUID().toString().substring(0, 13)
    }

    fun createCustomNotification(
        activity: Activity,
        title: String?,
        text: String?,
        imageUrl: String?,
        roomId: Int?
    ) {
        val remoteViews = RemoteViews(activity.packageName, R.layout.dialog_notification)
        remoteViews.setImageViewUri(R.id.iv_user_image, imageUrl?.toUri())
        remoteViews.setTextViewText(R.id.tv_title, title)
        remoteViews.setTextViewText(R.id.tv_message, text)

        val builder = NotificationCompat.Builder(activity, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_spika_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCustomContentView(remoteViews)
        with(NotificationManagerCompat.from(activity)) {
            // notificationId is a unique int for each notification that you must define
            roomId?.let { notify(it, builder.build()) }
        }
    }
}