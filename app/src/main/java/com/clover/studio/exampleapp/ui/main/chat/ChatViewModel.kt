package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.Settings
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.ui.main.MainStates
import com.clover.studio.exampleapp.ui.main.SingleRoomData
import com.clover.studio.exampleapp.ui.main.SingleRoomFetchFailed
import com.clover.studio.exampleapp.utils.*
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository,
    private val sseManager: SSEManager,
    private val uploadDownloadManager: UploadDownloadManager
) : BaseViewModel() {
    val messageSendListener = MutableLiveData<Event<ChatStatesEnum>>()
    val sendMessageDeliveredListener = MutableLiveData<Event<ChatStatesEnum>>()
    val roomWithUsersListener = MutableLiveData<Event<ChatStates>>()
    val roomDataListener = MutableLiveData<Event<MainStates>>()
    val userSettingsListener = MutableLiveData<Event<ChatStates>>()
    val roomNotificationListener = MutableLiveData<Event<ChatStates>>()
    val fileUploadListener = MutableLiveData<Event<ChatStates>>()
    val mediaUploadListener = MutableLiveData<Event<ChatStates>>()

    fun storeMessageLocally(message: Message) = viewModelScope.launch {
        try {
            repository.storeMessageLocally(message)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun sendMessage(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessage(jsonObject)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SEND_FAIL))
            }
            return@launch
        }

        messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SENT))
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefs.readUserId()
        }

        return userId
    }

    fun deleteLocalMessages(messages: List<Message>) = viewModelScope.launch {
        try {
            repository.deleteLocalMessages(messages)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun deleteLocalMessage(message: Message) = viewModelScope.launch {
        try {
            repository.deleteLocalMessage(message)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun sendMessagesSeen(roomId: Int) = viewModelScope.launch {
        try {
            repository.sendMessagesSeen(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun updateRoomVisitedTimestamp(chatRoom: ChatRoom) = viewModelScope.launch {
        try {
            repository.updatedRoomVisitedTimestamp(chatRoom)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) = viewModelScope.launch {
        try {
            repository.updateRoom(jsonObject, roomId, userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun isUserAdmin(roomId: Int, userId: Int): Boolean {
        var isAdmin = false

        runBlocking {
            try {
                isAdmin = repository.getRoomUserById(roomId, userId) == true
            } catch (ex: Exception) {
                Tools.checkError(ex)
            }
        }

        return isAdmin
    }

    fun getRoomAndUsers(roomId: Int) = liveData {
        emitSource(repository.getRoomWithUsersLiveData(roomId))
    }

    fun getPushNotificationStream(listener: SSEListener): Flow<Message> = flow {
        viewModelScope.launch {
            try {
                sseManager.startSSEStream(listener)
            } catch (ex: Exception) {
                Tools.checkError(ex)
                return@launch
            }
        }
    }

    fun getChatRoomAndMessageAndRecordsById(roomId: Int) = liveData {
        emitSource(repository.getChatRoomAndMessageAndRecordsById(roomId))
    }

    fun getSingleRoomData(roomId: Int) = viewModelScope.launch {
        try {
            roomDataListener.postValue(Event(SingleRoomData(repository.getSingleRoomData(roomId))))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                roomDataListener.postValue(Event(SingleRoomFetchFailed))
            }
            return@launch
        }
    }

    fun getRoomWithUsers(roomId: Int, message: Message) = viewModelScope.launch {
        try {
            roomNotificationListener.postValue(
                Event(
                    RoomNotificationData(
                        repository.getRoomWithUsers(
                            roomId
                        ), message
                    )
                )
            )
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                roomNotificationListener.postValue(Event(RoomWithUsersFailed))
            }
            return@launch
        }
    }

    fun muteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.muteRoom(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun unmuteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.unmuteRoom(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun getUserSettings() = viewModelScope.launch {
        try {
            val data = repository.getUserSettings()
            userSettingsListener.postValue(Event(UserSettingsFetched(data)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                userSettingsListener.postValue(Event(UserSettingsFetchFailed))
            }
            return@launch
        }
    }

    fun sendReaction(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendReaction(jsonObject)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                Timber.d("Exception: $ex")
            }
        }
    }

    /* TODO: Commented methods can later be used to delete reactions
    fun deleteReaction(recordId: Int, userId: Int) = viewModelScope.launch {
        try {
            repository.deleteReaction(recordId, userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                Timber.d("Exception: $ex")
            }
        }
    }

    fun deleteAllReactions(messageId: Int) = viewModelScope.launch {
        try {
            repository.deleteAllReactions(messageId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                Timber.d("Exception: $ex")
            }
        }
    }*/


    fun deleteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.deleteRoom(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun deleteMessage(messageId: Int, target: String) = viewModelScope.launch {
        try {
            repository.deleteMessage(messageId, target)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun editMessage(messageId: Int, jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.editMessage(messageId, jsonObject)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun uploadFile(activity: Activity, uri: Uri, uploadPieces: Long, fileStream: File) =
        viewModelScope.launch {
            try {
                uploadDownloadManager.uploadFile(
                    activity,
                    uri,
                    Const.JsonFields.FILE,
                    Const.JsonFields.FILE_TYPE,
                    uploadPieces,
                    fileStream,
                    false,
                    object : FileUploadListener {
                        override fun filePieceUploaded() {
                            fileUploadListener.postValue(Event(FilePieceUploaded))
                        }

                        override fun fileUploadError(description: String) {
                            fileUploadListener.postValue(Event(FileUploadError(description)))
                        }

                        override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                            fileUploadListener.postValue(
                                Event(FileUploadVerified(path, thumbId, fileId)))
                        }
                    })
            } catch (ex: Exception) {
                fileUploadListener.postValue(Event(FileUploadError(ex.message.toString())))
            }
        }

    fun uploadMedia(activity: Activity, uri: Uri, mimeType: String, uploadPieces: Long, fileStream: File, isThumbnail: Boolean) = viewModelScope.launch {
        try {
            uploadDownloadManager.uploadFile(
                activity,
                uri,
                mimeType,
                Const.JsonFields.AVATAR,
                uploadPieces,
                fileStream,
                isThumbnail,
                object : FileUploadListener {
                    override fun filePieceUploaded() {
                        mediaUploadListener.postValue(Event(MediaPieceUploaded))
                    }

                    override fun fileUploadError(description: String) {
                        mediaUploadListener.postValue(Event(MediaUploadError(description)))
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        mediaUploadListener.postValue(Event(MediaUploadVerified(path, thumbId, fileId, isThumbnail)))
                    }
                })
        } catch (ex: Exception) {
            mediaUploadListener.postValue(Event(MediaUploadError(ex.message.toString())))
        }
    }
}

sealed class ChatStates
class RoomWithUsersFetched(val roomWithUsers: RoomWithUsers) : ChatStates()
object RoomWithUsersFailed : ChatStates()
class RoomNotificationData(val roomWithUsers: RoomWithUsers, val message: Message) : ChatStates()
class UserSettingsFetched(val settings: List<Settings>) : ChatStates()
object UserSettingsFetchFailed : ChatStates()
object FilePieceUploaded : ChatStates()
class FileUploadError(val description: String) : ChatStates()
class FileUploadVerified(val path: String, val thumbId: Long, val fileId: Long) : ChatStates()
object MediaPieceUploaded : ChatStates()
class MediaUploadError(val description: String) :ChatStates()
class MediaUploadVerified(val path: String, val thumbId: Long, val fileId: Long, val isThumbnail: Boolean): ChatStates()

enum class ChatStatesEnum { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGE_DELIVERED, MESSAGE_DELIVER_FAIL }