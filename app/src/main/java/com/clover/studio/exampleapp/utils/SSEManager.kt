package com.clover.studio.exampleapp.utils

import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.networking.StreamingResponse
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.services.SSEService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.Gson
import kotlinx.coroutines.*
import okio.IOException
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class SSEManager @Inject constructor(
    private val sharedPrefs: SharedPreferencesRepository,
    private val sseService: SSEService,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val userDao: UserDao
) {
    private var job: Job? = null

    suspend fun startSSEStream() {
        val url =
            BuildConfig.SERVER_URL + Const.Networking.API_SSE_STREAM + "?accesstoken=" + sharedPrefs.readToken()

        openConnectionAndFetchEvents(url)
    }

    private suspend fun openConnectionAndFetchEvents(url: String) {
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
                var messageRecordsTimestamp = 0L
                var messageTimestamp = 0L
                var userTimestamp = 0L

                if (messageRecordsDao.getMessageRecordsLocally().isNotEmpty()) {
                    messageRecordsTimestamp =
                        messageRecordsDao.getMessageRecordsLocally().last().createdAt
                }

                if (messageDao.getMessagesLocally().isNotEmpty()) {
                    messageTimestamp = messageDao.getMessagesLocally().last().createdAt!!
                }

                if (userDao.getUsersLocally().isNotEmpty()) {
                    userTimestamp = userDao.getUsersLocally().last().createdAt?.toLong()!!
                }

                conn.connect() // Blocking function. Should run in background

                syncMessageRecords(messageRecordsTimestamp)
                syncMessages(messageTimestamp)
                syncUsers(userTimestamp)

                val inputReader = conn.inputStream.bufferedReader()

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
                                val gson = Gson()
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
                                if (response.data?.type == Const.JsonFields.NEW_MESSAGE) {
                                    response.data?.message?.let { messageDao.insert(it) }
                                } else if (response.data?.type == Const.JsonFields.NEW_MESSAGE_RECORD) {
                                    response.data?.messageRecord?.let { messageRecordsDao.insert(it) }
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
                    openConnectionAndFetchEvents(url)
                }
                Tools.checkError(ex)
            }
        }
    }

    private suspend fun syncMessageRecords(timestamp: Long) {
        val response =
            sseService.syncMessageRecords(getHeaderMap(sharedPrefs.readToken()), timestamp)

        for (record in response.data.messageRecords)
            messageRecordsDao.insert(record)
    }

    private suspend fun syncMessages(timestamp: Long) {
        val response =
            sseService.syncMessages(
                getHeaderMap(sharedPrefs.readToken()),
                timestamp
            )

        if (response.data?.list?.isNotEmpty() == true) {
            for (message in response.data.list) {
                messageDao.insert(message)
            }
        }
    }

    private suspend fun syncUsers(timestamp: Long) {
        val response =
            sseService.syncUsers(
                getHeaderMap(sharedPrefs.readToken()),
                timestamp
            )

        if (response.data?.list?.isNotEmpty() == true) {
            for (user in response.data.list) {
                userDao.insert(user)
            }
        }
    }
}