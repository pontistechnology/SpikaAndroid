@file:Suppress("BlockingMethodInNonBlockingContext")

package com.clover.studio.exampleapp.utils

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.MainApplication
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.networking.responses.StreamingResponse
import com.clover.studio.exampleapp.data.repositories.SSERepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.helpers.GsonProvider
import com.google.gson.Gson
import kotlinx.coroutines.*
import okio.IOException
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection


class SSEManager @Inject constructor(
    private val repo: SSERepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository,
) {
    private var job: Job? = null
    private var listener: SSEListener? = null

    fun setupListener(listener: SSEListener) {
        this.listener = listener
    }

    suspend fun startSSEStream() {
        val url =
            BuildConfig.SERVER_URL + Const.Networking.API_SSE_STREAM + "?accesstoken=" + sharedPrefs.readToken()

        openConnectionAndFetchEvents(url)
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
                        launch { repo.syncUsers() }
                        launch { repo.syncRooms() }
                    } else {
                        launch { repo.syncUsers() }
                        launch { repo.syncRooms() }
                    }

                    sharedPrefs.writeFirstSSELaunch()
                }

                // run while the coroutine is active
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
                                        response.data?.message?.let { repo.writeMessages(it) }
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
                                    }
                                    Const.JsonFields.UPDATE_MESSAGE -> {
                                        response.data?.message?.let { repo.writeMessages(it) }
                                    }
                                    Const.JsonFields.DELETE_MESSAGE -> {
                                        // We replace old message with new one displaying "Deleted
                                        // message" in its text field
                                        response.data?.message?.let { repo.writeMessages(it) }
                                    }
                                    Const.JsonFields.NEW_MESSAGE_RECORD -> {
                                        response.data?.messageRecord?.let {
                                            repo.writeMessageRecord(
                                                it
                                            )
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