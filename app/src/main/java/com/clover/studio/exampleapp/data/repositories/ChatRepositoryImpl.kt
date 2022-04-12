package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.data.models.networking.StreamingResponse
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService,
    private val messageDao: MessageDao,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ChatRepository {
    override suspend fun sendMessage(jsonObject: JsonObject): Message =
        chatService.sendMessage(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)

    override suspend fun getMessages(roomId: String) {
        val response = chatService.getMessages(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

        if (response.data?.list != null) {
            for (message in response.data.list) {
                messageDao.insert(message)
            }
        }
    }

    override suspend fun getMessagesLiveData(roomId: Int): LiveData<List<Message>> =
        messageDao.getMessages(roomId)

    override suspend fun getMessagesTimestamp(timestamp: Int): MessageResponse =
        chatService.getMessagesTimestamp(getHeaderMap(sharedPrefsRepo.readToken()), timestamp)

    override suspend fun sendMessageDelivered(jsonObject: JsonObject) =
        chatService.sendMessageDelivered(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)

    override suspend fun storeMessageLocally(message: Message) {
        messageDao.insert(message)
    }

    override suspend fun deleteLocalMessages(messages: List<Message>) {
        if (messages.isNotEmpty()) {
            for (message in messages) {
                messageDao.deleteMessage(message)
            }
        }
    }

    override suspend fun getPushNotificationStream() {
        val url =
            BuildConfig.SERVER_URL + Const.Networking.API_SSE_STREAM + "?accesstoken=" + sharedPrefsRepo.readToken()

        // Gets HttpURLConnection. Blocking function.  Should run in background
        val conn = (URL(url).openConnection() as HttpURLConnection).also {
            it.setRequestProperty("Accept", "text/event-stream") // set this Header to stream
            it.doInput = true // enable inputStream
        }

        withContext(Dispatchers.IO) {
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
                        val jsonObject = JSONObject("{$line}")
                        val gson = Gson()
                        val message =
                            gson.fromJson(jsonObject.toString(), StreamingResponse::class.java)
                        messageDao.insert(message.data?.message!!)
                    }
                    line.isEmpty() -> { // empty line, finished block. Emit the event
                        Timber.d("Emitting event")
                    }
                }
            }
        }
    }
}

interface ChatRepository {
    suspend fun sendMessage(jsonObject: JsonObject): Message
    suspend fun getMessages(roomId: String)
    suspend fun getMessagesLiveData(roomId: Int): LiveData<List<Message>>
    suspend fun getMessagesTimestamp(timestamp: Int): MessageResponse
    suspend fun sendMessageDelivered(jsonObject: JsonObject)
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
    suspend fun getPushNotificationStream()
}