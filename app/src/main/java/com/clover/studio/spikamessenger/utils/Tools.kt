package com.clover.studio.spikamessenger.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.NavOptions
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.models.FileMetadata
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.PhoneUser
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepositoryImpl
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortPrivateChats
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.vanniktech.emoji.EmojiTheming
import com.vanniktech.emoji.emojisCount
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random


const val TO_MEGABYTE = 1000000
const val TO_KILOBYTE = 1000
const val TO_BYTES = 8
const val TO_HOURS = 3600
const val TO_MINUTES = 60
const val VIDEO_SIZE_LIMIT = 128
const val TOKEN_EXPIRED_CODE = 401

const val BIG_EMOJI_SIZE = 144
const val MEDIUM_EMOJI_SIZE = 104
const val SMALL_EMOJI_SIZE = 80

object Tools {

    private var density = 1f

    fun checkError(ex: Exception): Boolean {
        when (ex) {
            is IllegalArgumentException -> Timber.d("IllegalArgumentException ${ex.message}")
            is IOException -> Timber.d("IOException ${ex.message}")
            is HttpException ->
                if (TOKEN_EXPIRED_CODE == ex.code()) {
                    Timber.d("Token error: ${ex.code()} ${ex.message}")
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

    fun copyStreamToFile(
        inputStream: InputStream,
        extension: String,
        fileName: String = ""
    ): File {
        var tempFileName = fileName
        if (tempFileName.isEmpty()) {
            tempFileName =
                "tempFile${System.currentTimeMillis()}.${extension.substringAfterLast("/")}"
        }
        val outputFile = File(MainApplication.appContext.cacheDir, tempFileName)
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

    fun calculateFileSize(size: Long): String {
        val df = DecimalFormat("#.##")
        if (size > TO_MEGABYTE) {
            return df.format(size.toFloat().div(TO_MEGABYTE)) + "MB"
        }
        return df.format(size.toFloat().div(TO_KILOBYTE)) + "KB"
    }

    @Throws(IOException::class)
    fun createImageFile(activity: Activity?): File {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )
    }

    private fun calculateSHA256FileHash(updateFile: File?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = updateFile?.inputStream()
        val buffer = ByteArray(8192)
        var bytesRead: Int

        do {
            bytesRead = inputStream?.read(buffer)!!
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
            }
        } while (bytesRead != -1)

        inputStream.close()

        val hashBytes = digest.digest()
        val hexString = StringBuilder()

        for (byte in hashBytes) {
            val hex = String.format("%02x", byte)
            hexString.append(hex)
        }

        return hexString.toString()
    }

    /**
     * Method handles resizing of provided Bitmap depending on if it is a thumbnail or image file.
     * Image files will be resized so that the shorter side doesn't exceed 1080dp and the longer
     * side conforms to the aspect ratio.
     *
     * Thumbnails are the same as above, but they shouldn't exceed 256dp on the shorter side.
     *
     * After the resize work, the image will get rotated if necessary.
     *
     * @param context Context of the fragment or activity
     * @param selectedImage Uri of the image that needs to be modified
     * @param thumbnail Boolean which decides if file or thumbnail operations should be carried out
     */
    @Throws(IOException::class)
    fun handleSamplingAndRotationBitmap(
        context: Context,
        selectedImage: Uri?,
        thumbnail: Boolean
    ): Bitmap? {
        val maxValue = if (thumbnail) 256f else 1080f
        val maxShorterSide = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            maxValue,
            context.resources.displayMetrics
        )
        val bitmap: Bitmap = when (selectedImage) {
            is Uri -> {
                val inputStream = context.contentResolver.openInputStream(selectedImage)
                BitmapFactory.decodeStream(inputStream)
            }

            else -> null
        } ?: return null

        // Determine the new dimensions of the image based on the maximum shorter side and the aspect ratio
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        val newWidth: Int
        val newHeight: Int
        if (min(originalWidth, originalHeight) > maxShorterSide) {
            if (originalWidth < originalHeight) {
                newWidth = (maxShorterSide * aspectRatio).toInt()
                newHeight = maxShorterSide.toInt()
            } else {
                newWidth = maxShorterSide.toInt()
                newHeight = (maxShorterSide / aspectRatio).toInt()
            }
        } else {
            newWidth = originalWidth
            newHeight = originalHeight
        }
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        return rotateImageIfRequired(context, resizedBitmap, selectedImage!!)
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
        currentPhotoLocation: Uri,
        extension: String
    ): String {
        val sha256FileHash: String?
        val inputStream =
            MainApplication.appContext.contentResolver.openInputStream(currentPhotoLocation)
        sha256FileHash =
            calculateSHA256FileHash(copyStreamToFile(inputStream!!, extension))

        inputStream.close()

        return sha256FileHash
    }

    fun getFilePathUrl(fileId: Long): String {
        return "${BuildConfig.SERVER_URL}${Const.Networking.API_GET_FILE_FROM_ID}$fileId"
    }

    fun getRelativeTimeSpan(startDate: Long): CharSequence? {
        return DateUtils.getRelativeTimeSpanString(
            startDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
    }

    fun getRoomTime(startDate: Long): CharSequence? {
        val currentTimeMillis = System.currentTimeMillis()
        val timeDifference = currentTimeMillis - startDate

        return when {
            timeDifference >= DateUtils.WEEK_IN_MILLIS -> {
                SimpleDateFormat("dd.MM.", Locale.getDefault()).format(startDate)
            }

            timeDifference >= DateUtils.DAY_IN_MILLIS -> {
                DateUtils.formatDateTime(
                    MainApplication.appContext,
                    startDate,
                    DateUtils.FORMAT_SHOW_WEEKDAY
                )
            }

            else -> {
                DateUtils.getRelativeTimeSpanString(
                    startDate,
                    currentTimeMillis,
                    DateUtils.MINUTE_IN_MILLIS
                )
            }
        }
    }

    fun generateRandomId(): String {
        return UUID.randomUUID().toString().substring(0, 13)
    }

    fun convertDurationMillis(time: Long): String {
        val millis: Long = time
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                time
            )
        )
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                time
            )

        )
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun convertDurationInSeconds(time: Long): String {
        val hours = time / TO_HOURS
        val minutes = (time % TO_HOURS) / TO_MINUTES
        val seconds = time % TO_MINUTES

        return when {
            hours > 0 -> String.format("%2d h %2d min", hours, minutes)
            minutes > 0 -> String.format("%2d min", minutes)
            else -> String.format("%2d s", seconds)
        }
    }

    fun getVideoSize(duration: Long, bitRate: Long): Boolean {
        val videoSIze = (duration / TO_KILOBYTE) * bitRate / TO_BYTES
        return videoSIze / TO_MEGABYTE > VIDEO_SIZE_LIMIT
    }

    fun generateRandomInt(): Int {
        return Random.nextInt(Int.MIN_VALUE, 0)
    }

    fun downloadFile(context: Context, message: Message) {
        try {
            val tmp = this.getFilePathUrl(message.body!!.fileId!!)
            val request = DownloadManager.Request(Uri.parse(tmp))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setTitle(
                message.body.file?.fileName ?: "${
                    MainApplication.appContext.getString(
                        R.string.spika
                    )
                }.jpg"
            )
            request.setDescription(context.getString(R.string.file_is_downloading))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                message.body.file!!.fileName
            )
            val manager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(
                context,
                context.getString(R.string.file_is_downloading),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Timber.d("$e")
        }
    }

    fun navigateToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", MainApplication.appContext.packageName, null)
        intent.data = uri
        MainApplication.appContext.startActivity(intent)
    }

    fun createTemporaryMessage(
        id: Int,
        localUserId: Int?,
        roomId: Int,
        messageType: String,
        messageBody: MessageBody
    ): Message {
        return Message(
            id,
            localUserId,
            0,
            -1,
            0,
            roomId,
            messageType,
            messageBody,
            System.currentTimeMillis(),
            null,
            null,
            null,
            generateRandomId(),
            Resource.Status.LOADING.toString(),
            null,
            isForwarded = false
        )
    }

    fun fullDateFormat(dateTime: Long): String? {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm aa", Locale.getDefault())
        return simpleDateFormat.format(dateTime)
    }

    /**
     * Code generates video file in mp4 format by decoding it piece by piece
     *
     * @param srcUri Source URI to be converted to mp4 format
     * @param dstPath Destination path for the converted video file
     * @param startMs Can be used for start time when trimming a video
     * @param endMs Can be used for end time when trimming video
     * @param useAudio Boolean which decides if new file will have audio
     * @param useVideo Boolean which decides if new file will have video
     */
    @Throws(IOException::class)
    fun genVideoUsingMuxer(
        srcUri: Uri?,
        dstPath: String?,
        startMs: Long = 0L,
        endMs: Long = 0L,
        useAudio: Boolean = true,
        useVideo: Boolean = true
    ) {
        // Set up MediaExtractor to read from the source.
        val extractor = MediaExtractor()
        extractor.setDataSource(MainApplication.appContext, srcUri!!, null)
        val trackCount = extractor.trackCount
        // Set up MediaMuxer for the destination.
        val muxer = MediaMuxer(dstPath!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        val indexMap: HashMap<Int, Int> = HashMap(trackCount)
        var bufferSize = -1
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            var selectCurrentTrack = false
            if (mime!!.startsWith("audio/") && useAudio) {
                selectCurrentTrack = true
            } else if (mime.startsWith("video/") && useVideo) {
                selectCurrentTrack = true
            }
            if (selectCurrentTrack) {
                extractor.selectTrack(i)
                val dstIndex = muxer.addTrack(format)
                indexMap[i] = dstIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    bufferSize = newSize.coerceAtLeast(bufferSize)
                }
            }
        }
        if (bufferSize < 0) {
            bufferSize = DEFAULT_BUFFER_SIZE
        }
        // Set up the orientation and starting time for extractor.
        val retrieverSrc = MediaMetadataRetriever()
        retrieverSrc.setDataSource(MainApplication.appContext, srcUri)
        val degreesString = retrieverSrc.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )
        if (degreesString != null) {
            val degrees = degreesString.toInt()
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees)
            }
        }

        if (startMs > 0) {
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }
        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        val offset = 0
        var trackIndex: Int
        val dstBuf: ByteBuffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        muxer.start()
        while (true) {
            bufferInfo.offset = offset
            bufferInfo.size = extractor.readSampleData(dstBuf, offset)
            if (bufferInfo.size < 0) {
                Timber.tag("LOGTAG").d("Saw input EOS.")
                bufferInfo.size = 0
                break
            } else {
                bufferInfo.presentationTimeUs = extractor.sampleTime
                // Code used when trimming videos
                if (endMs > 0 && bufferInfo.presentationTimeUs > endMs * 1000L) {
                    Timber.tag("LOGTAG").d("The current sample is over the trim end time.")
                    break
                } else {
                    bufferInfo.flags = extractor.sampleFlags
                    trackIndex = extractor.sampleTrackIndex
                    muxer.writeSampleData(
                        indexMap[trackIndex]!!, dstBuf,
                        bufferInfo
                    )
                    extractor.advance()
                }
            }
        }
        muxer.stop()
        muxer.release()
    }

    @SuppressLint("Range")
    fun fetchPhonebookContacts(context: Context, countryCode: String?): List<PhoneUser>? {
        if (context.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_CONTACTS
                )
            } == PackageManager.PERMISSION_GRANTED) {
            val phoneUsers: MutableList<PhoneUser> = ArrayList()
            val phones: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            while (phones?.moveToNext()!!) {
                val name =
                    phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber =
                    phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                val phoneUser = PhoneUser(
                    name,
                    formatE164Number(context, countryCode, phoneNumber).toString()
                )
                phoneUsers.add(phoneUser)
                Timber.d("Adding phone user: ${phoneUser.name} ${phoneUser.number}")
            }
            DatabaseUtils.dumpCursor(phones)

            return phoneUsers
        } else return null
    }

    fun getContactsNumbersHashed(
        context: Context,
        countryCode: String?,
        phoneUser: List<PhoneUser>
    ): Set<String> {
        val phoneUserSet: MutableSet<String> = ArraySet()

        for (user in phoneUser) {
            phoneUserSet.add(
                hashString(
                    formatE164Number(context, countryCode, user.number).toString()
                )
            )
        }

        return phoneUserSet
    }

    fun dp(value: Float, context: Context): Int {
        if (density == 1f) {
            checkDisplaySize(context)
        }
        return if (value == 0f) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    private fun checkDisplaySize(context: Context) {
        try {
            density = context.resources.displayMetrics.density
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getMediaFile(context: Context, message: Message): String {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var mediaPath = "$directory/${message.localId}.${Const.FileExtensions.JPG}"
        val file = File(mediaPath)
        if (!file.exists()) {
            mediaPath = message.body?.thumb?.id?.let { imagePath ->
                getFilePathUrl(
                    imagePath
                )
            }.toString()
        }
        return mediaPath
    }

    fun deleteTemporaryMedia(context: Context) {
        val imagesDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imagesDirectory?.listFiles { _, name -> name.startsWith("JPEG") }?.forEach { file ->
            file.delete()
        }

        val videoDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        videoDirectory?.listFiles { _, name -> name.startsWith("VIDEO") }?.forEach { file ->
            file.delete()
        }
    }

    fun getMetadata(
        mediaUri: Uri,
        mimeType: String,
        isThumbnail: Boolean
    ): FileMetadata? {
        var fileMetadata: FileMetadata? = null

        val height: Int
        val width: Int
        val time: Int

        val inputStream = MainApplication.appContext.contentResolver.openInputStream(mediaUri)

        val fileStream = copyStreamToFile(
            inputStream = inputStream!!,
            getFileMimeType(MainApplication.appContext, mediaUri)!!
        )

        if (mimeType.contains(Const.JsonFields.IMAGE_TYPE) || isThumbnail) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(fileStream.absolutePath, options)
            height = options.outHeight
            width = options.outWidth

            fileMetadata = FileMetadata(width, height, 0)
            Timber.d("File metadata: $fileMetadata")
        } else if (mimeType.contains(Const.JsonFields.VIDEO_TYPE)) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(MainApplication.appContext, mediaUri)
            time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                .toInt() / 1000
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt()
            height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                    .toInt()
            retriever.release()

            fileMetadata = FileMetadata(width, height, time)
            Timber.d("File metadata: $fileMetadata")
        }
        inputStream.close()
        return fileMetadata
    }

    fun openTermsAndConditions(activity: Activity) {
        val uri =
            Uri.parse(Const.Urls.TERMS_AND_CONDITIONS)

        val intent = Intent(Intent.ACTION_VIEW, uri)
        activity.startActivity(intent)
    }

    fun clearUserData(activity: Activity) {
        val sharedPrefs = SharedPreferencesRepositoryImpl(MainApplication.appContext)
        sharedPrefs.clearSharedPrefs()
        AppDatabase.nukeDb()
        deleteTemporaryMedia(MainApplication.appContext)
        startOnboardingActivity(activity, false)
    }

    fun getFileType(uri: Uri): String {
        val mimeType = getFileMimeType(MainApplication.appContext, uri)
        return when {
            mimeType?.contains(Const.JsonFields.SVG_TYPE) == true -> Const.JsonFields.FILE_TYPE
            mimeType?.contains(Const.JsonFields.AVI_TYPE) == true -> Const.JsonFields.FILE_TYPE
            mimeType?.contains(Const.JsonFields.MOV_TYPE) == true -> Const.JsonFields.FILE_TYPE
            mimeType?.contains(Const.JsonFields.IMAGE_TYPE) == true -> Const.JsonFields.IMAGE_TYPE
            mimeType?.contains(Const.JsonFields.VIDEO_TYPE) == true -> Const.JsonFields.VIDEO_TYPE
            mimeType?.contains(Const.JsonFields.AUDIO_TYPE) == true -> Const.JsonFields.AUDIO_TYPE
            else -> Const.JsonFields.FILE_TYPE
        }
    }

    fun forbiddenMimeTypes(fileMimeType: String): Boolean {
        return (fileMimeType.contains(Const.JsonFields.SVG_TYPE) ||
                fileMimeType.contains(Const.JsonFields.AVI_TYPE)) ||
                fileMimeType.contains(Const.JsonFields.MOV_TYPE)
    }

    fun getFileMimeType(context: Context?, uri: Uri): String? {
        val cR: ContentResolver = context!!.contentResolver
        return cR.getType(uri)
    }

    fun getFileNameFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor =
            MainApplication.appContext.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return ""
    }

    fun createCustomNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.nav_slide_in_right)
            .setExitAnim(R.anim.nav_slide_out_left)
            .setPopEnterAnim(R.anim.nav_slide_in_left)
            .setPopExitAnim(R.anim.nav_slide_out_right)
            .build()
    }

    fun setTheme(theme: String): Int {
        return when (theme) {
            Const.Themes.MINT_THEME -> {
                R.style.Theme_App_LightGreen
            }

            Const.Themes.NEON_THEME -> {
                R.style.Theme_App_Neon
            }

            Const.Themes.BASIC_THEME_NIGHT -> {
                R.style.Theme_App_DarkMarine
            }

            else -> {
                R.style.Theme_ExampleApp
            }
        }
    }

    fun handleCopyAction(text: String) {
        val clipboard =
            MainApplication.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            MainApplication.appContext,
            MainApplication.appContext.getString(R.string.text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun getPlaceholderImage(roomType: String): Int {
        return if (roomType == Const.JsonFields.GROUP) {
            R.drawable.img_group_avatar
        } else {
            R.drawable.img_user_avatar
        }
    }

    fun setEmojiViewTheme(context: Context): EmojiTheming {
        return EmojiTheming(
            primaryColor = ColorHelper.getPrimaryColor(context),
            secondaryColor = ColorHelper.getPrimaryTextColor(context),
            backgroundColor = ColorHelper.getSecondaryColor(context)
        )
    }

    fun getEmojiSize(messageText: String): Int {
        return when (messageText.emojisCount()) {
            1 -> {
                BIG_EMOJI_SIZE
            }

            in 2..3 -> {
                MEDIUM_EMOJI_SIZE
            }

            else -> {
                SMALL_EMOJI_SIZE
            }
        }
    }

    fun transformPrivateList(
        context: Context,
        list: List<UserAndPhoneUser>
    ): MutableList<PrivateGroupChats> =
        list.map { PrivateGroupChats(private = it, group = null) }
            .toMutableList()
            .sortPrivateChats(context)
            .toMutableList()

    fun transformGroupList(list: List<RoomWithMessage>): MutableList<PrivateGroupChats> {
        val tmp = mutableListOf<PrivateGroupChats>()
        list.forEach { responseGroup ->
            val element = PrivateGroupChats(
                private = null,
                group = responseGroup
            )
            tmp.add(element)
        }
        return tmp.toMutableList()
    }

    fun setUpSearchBar(context: Context, searchView: androidx.appcompat.widget.SearchView){
        searchView.apply {
            queryHint = context.getString(R.string.contact_message_search)
            setBackgroundResource(R.drawable.bg_input)
            backgroundTintList =  ColorStateList.valueOf(ColorHelper.getFourthAdditionalColorWithAlpha(context))
            setIconifiedByDefault(false)

            val searchPlate =
                this.findViewById<View>(androidx.appcompat.R.id.search_plate)
            searchPlate.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))

            val closeImageView = this.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            closeImageView.imageTintList = ColorStateList.valueOf(ColorHelper.getPrimaryTextColor(context))
        }
    }
}
