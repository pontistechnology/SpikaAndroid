@file:Suppress("BlockingMethodInNonBlockingContext")

package com.clover.studio.spikamessenger.utils

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.entity.RecordMessage
import com.clover.studio.spikamessenger.data.models.networking.responses.StreamingResponse
import com.clover.studio.spikamessenger.data.repositories.SSERepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.utils.helpers.GsonProvider
import kotlinx.coroutines.*
import okio.IOException
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class SSEManager @Inject constructor(
    private val repo: SSERepository,
    private val sharedPrefs: SharedPreferencesRepository,
) {
    private var job: Job? = null
    private var listener: SSEListener? = null
    private lateinit var url: String

    fun setupListener(listener: SSEListener) {
        this.listener = listener
    }

    suspend fun startSSEStream() {
        url =
            BuildConfig.SERVER_URL + Const.Networking.API_SSE_STREAM + "?accesstoken=" + sharedPrefs.readToken()

        openConnectionAndFetchEvents(url)
    }

    // Called only in the application class. It will check if the job is completed or cancelled
    // after some downtime and restart the connection if needed.
    suspend fun checkJobAndContinue() {
        Timber.d("SSE Job status: ${job?.isActive}, ${job?.isCompleted}, ${job?.isCancelled}")
        if (job?.isCompleted == true || job?.isCancelled == true) {
            Timber.d("Launching connection")
            openConnectionAndFetchEvents(url)
        }
    }

    private suspend fun openConnectionAndFetchEvents(url: String) {
        if (!MainApplication.isInForeground) return

        if (job != null) {
            job?.cancel()
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Opening connection for SSE events")
            Timber.d("URL check $url")
            try {
                // Gets HttpURLConnection. Blocking function.  Should run in background
                val conn = (URL(url).openConnection() as HttpURLConnection).also {
                    it.setRequestProperty(
                        "Accept",
                        "text/event-stream"
                    ) // set this Header to stream
                    it.doInput = true // enable inputStream
                }

                // Fetch local timestamps for syncing later. This will handle potential missing data
                // in between calls. After this, open the connection to the SSE
                conn.connect() // Blocking function. Should run in background

                val inputReader = conn.inputStream.bufferedReader()

                // Check that connection returned 200 OK and then launch all the sync calls
                // asynchronously
                if (conn.responseCode == HttpsURLConnection.HTTP_OK) {
                    if (!sharedPrefs.isFirstSSELaunch()) {
                        launch { repo.syncMessageRecords() }
                        launch { repo.syncMessages() }
                        launch { repo.syncContacts() }
                        launch { repo.syncUsers() }
                        launch { repo.syncRooms() }
                    } else {
                        repo.getAppMode()
                        launch { repo.syncUsers() }
                        launch { repo.syncContacts() }
                        launch { repo.syncRooms() }
                    }

                    sharedPrefs.writeFirstSSELaunch()
                }

                // Run while the coroutine is active
                while (isActive) {
                    val line =
                        inputReader.readLine() // Blocking function. Read stream until \n is found
                    Timber.d(line)
                    when {
                        line.startsWith("message:") -> { // get event name
                            Timber.d("Copy message event $line")
                        }

                        line.startsWith("data:") -> { // get data
                            Timber.d("Copy data event $line")

                            var response: StreamingResponse? = null
                            try {
                                val jsonObject = JSONObject("{$line}")
                                val gson = GsonProvider.gson
                                response =
                                    gson.fromJson(
                                        jsonObject.toString(),
                                        StreamingResponse::class.java
                                    )
                            } catch (ex: Exception) {
                                Tools.checkError(ex)
                            }

                            if (response != null) {
                                Timber.d("Response type: ${response.data?.type}")
                                when (response.data?.type) {
                                    Const.JsonFields.NEW_MESSAGE -> {
                                        response.data?.message?.id?.let {
                                            repo.sendMessageDelivered(
                                                it
                                            )
                                        }
                                        response.data?.message?.let {
                                            listener?.newMessageReceived(
                                                it
                                            )
                                        }
                                        repo.getUnreadCount()

                                        response.data?.message?.let { repo.writeMessages(it) }
                                    }

                                    Const.JsonFields.UPDATE_MESSAGE, Const.JsonFields.DELETE_MESSAGE -> {
                                        // We replace old message with new one displaying "Deleted
                                        // message" or  we add "edited" in its text field
                                        response.data?.message?.let { repo.writeMessages(it) }
                                    }

                                    Const.JsonFields.NEW_MESSAGE_RECORD -> {
                                        response.data?.let {
                                            if (it.messageRecord != null) {
                                                val record = MessageRecords(
                                                    id = it.messageRecord.id,
                                                    messageId = it.messageRecord.messageId,
                                                    userId = it.messageRecord.userId,
                                                    type = it.messageRecord.type,
                                                    reaction = it.messageRecord.reaction,
                                                    modifiedAt = it.messageRecord.modifiedAt,
                                                    createdAt = it.messageRecord.createdAt,
                                                    isDeleted = it.messageRecord.isDeleted,
                                                    recordMessage = RecordMessage(
                                                        id = it.messageRecord.messageId.toLong(),
                                                        totalUserCount = it.totalUserCount ?: 0,
                                                        deliveredCount = it.deliveredCount ?: 0,
                                                        seenCount = it.seenCount ?: 0,
                                                        roomId = it.messageRecord.roomId,
                                                    )
                                                )
                                                repo.writeMessageRecord(record)
                                            }
                                        }
                                        response.data?.messageRecord?.let {
                                            // Check if message record is seen event and if it is my id
                                            // Remove the notification for that room if it exists
                                            if (Const.JsonFields.SEEN == it.type && sharedPrefs.readUserId() == it.userId) {
                                                it.roomId.let { roomId ->
                                                    NotificationManagerCompat.from(MainApplication.appContext)
                                                        .cancel(
                                                            roomId
                                                        )
                                                }
                                            }
                                        }
                                    }

                                    Const.JsonFields.DELETE_MESSAGE_RECORD -> {
                                        response.data?.messageRecord?.let {
                                            repo.deleteMessageRecord(
                                                it
                                            )
                                        }
                                    }

                                    Const.JsonFields.USER_UPDATE -> {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            response.data?.user?.let { repo.writeUser(it) }
                                        }
                                    }

                                    Const.JsonFields.NEW_ROOM -> {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            response.data?.room?.let { repo.writeRoom(it) }
                                        }
                                    }

                                    Const.JsonFields.UPDATE_ROOM -> {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            response.data?.room?.let { repo.writeRoom(it) }
                                        }
                                    }

                                    Const.JsonFields.DELETE_ROOM -> {
                                        response.data?.room?.let { repo.deleteRoom(it.roomId) }
                                    }

                                    Const.JsonFields.SEEN_ROOM -> {
                                        response.data?.roomId?.let { repo.resetUnreadCount(it) }
                                    }
                                }
                            }
                        }

                        line.isEmpty() -> { // empty line, finished block. Emit the event
                            Timber.d("Emitting event")
                        }
                    }
                }
            } catch (ex: Exception) {
                if (ex is IOException) {
                    Timber.d("IOException ${ex.message} ${ex.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        object : CountDownTimer(5000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                Timber.d("Timer tick $millisUntilFinished")
                            }

                            override fun onFinish() {
                                CoroutineScope(Dispatchers.IO).launch {
                                    Timber.d("Launching connection")
                                    openConnectionAndFetchEvents(url)
                                }
                            }
                        }.start()
                    }
                }
                Tools.checkError(ex)
            }
        }
    }
}

interface SSEListener {
    fun newMessageReceived(message: Message)
}
