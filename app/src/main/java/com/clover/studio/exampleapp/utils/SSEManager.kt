package com.clover.studio.exampleapp.utils

import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.models.networking.StreamingResponse
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
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
    private val messageDao: MessageDao
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

                conn.connect() // Blocking function. Should run in background

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
                            Timber.d("Copy data event ${line.startsWith("data:")}")
                            try {
                                val jsonObject = JSONObject("{$line}")
                                val gson = Gson()
                                val message =
                                    gson.fromJson(
                                        jsonObject.toString(),
                                        StreamingResponse::class.java
                                    )
                                messageDao.insert(message.data?.message!!)
                            } catch (ex: Exception) {
                               Tools.checkError(ex)
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
}