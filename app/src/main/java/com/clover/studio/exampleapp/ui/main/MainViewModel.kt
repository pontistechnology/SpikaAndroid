package com.clover.studio.exampleapp.ui.main

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.entity.*
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.responses.AuthResponse
import com.clover.studio.exampleapp.data.models.networking.responses.RoomResponse
import com.clover.studio.exampleapp.data.repositories.MainRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.ui.main.chat.ChatStates
import com.clover.studio.exampleapp.ui.main.chat.MediaPieceUploaded
import com.clover.studio.exampleapp.ui.main.chat.MediaUploadError
import com.clover.studio.exampleapp.ui.main.chat.MediaUploadVerified
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository,
    private val sseManager: SSEManager,
    private val uploadDownloadManager: UploadDownloadManager
) : BaseViewModel() {
    val usersListener = MutableLiveData<Event<Resource<AuthResponse>>>()
    val checkRoomExistsListener = MutableLiveData<Event<Resource<RoomResponse?>>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()
    val userUpdateListener = MutableLiveData<Event<MainStates>>()
    val roomWithUsersListener = MutableLiveData<Event<Resource<RoomWithUsers>>>()
    val roomDataListener = MutableLiveData<Event<Resource<RoomAndMessageAndRecords>>>()
    val roomNotificationListener = MutableLiveData<Event<RoomNotificationData>>()
    val blockedListListener = MutableLiveData<Event<Resource<List<User>>>>()
    val mediaUploadListener = MutableLiveData<Event<ChatStates>>()

    fun getLocalUser() = liveData {
        val localUserId = sharedPrefsRepo.readUserId()

        if (localUserId != null && localUserId != 0) {
            emitSource(repository.getUserByID(localUserId))
        } else {
            return@liveData
        }
    }

    fun checkIfFirstSSELaunch(): Boolean {
        var isFirstLaunch = false

        viewModelScope.launch {
            isFirstLaunch = sharedPrefsRepo.isFirstSSELaunch()
        }
        return isFirstLaunch
    }

    fun setupSSEManager(listener: SSEListener) {
        sseManager.setupListener(listener)
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefsRepo.readUserId()
        }
        return userId
    }

    suspend fun checkIfUserInPrivateRoom(userId: Int): Int? {
        return if (repository.checkIfUserInPrivateRoom(userId) != null) {
            repository.checkIfUserInPrivateRoom(userId)!!
        } else null
    }

    fun checkIfRoomExists(userId: Int) = viewModelScope.launch {
        resolveResponseStatus(checkRoomExistsListener, repository.getRoomById(userId))
//        checkRoomExistsListener.postValue(Event(repository.getRoomById(userId)))
    }

    // TODO remove try - catch
    fun createNewRoom(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            val roomData = repository.createNewRoom(jsonObject).data?.room
            createRoomListener.postValue(Event(RoomCreated(roomData!!)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                createRoomListener.postValue(Event(RoomCreateFailed))
            }
            return@launch
        }
    }

    fun getPushNotificationStream(): Flow<Any> = flow {
        viewModelScope.launch {
            try {
                sseManager.startSSEStream()
            } catch (ex: Exception) {
                if (Tools.checkError(ex)) {
                    setTokenExpiredTrue()
                }
                return@launch
            }
        }
    }

    fun getUserAndPhoneUser(localId: Int) = repository.getUserAndPhoneUser(localId)

    fun getChatRoomAndMessageAndRecords() = repository.getChatRoomAndMessageAndRecords()

    fun getRoomByIdLiveData(roomId: Int) = repository.getRoomByIdLiveData(roomId)

    fun getSingleRoomData(roomId: Int) =
        viewModelScope.launch {
            roomDataListener.postValue(Event(repository.getSingleRoomData(roomId)))
        }

    fun getRoomWithUsers(roomId: Int) =
        viewModelScope.launch {
            roomWithUsersListener.postValue(Event(repository.getRoomWithUsers(roomId)))
        }

    fun getRoomWithUsers(roomId: Int, message: Message) = viewModelScope.launch {
        val response = repository.getRoomWithUsers(roomId)
        roomNotificationListener.postValue(
            Event(
                RoomNotificationData(
                    response,
                    message
                )
            )
        )
    }

    fun updatePushToken(jsonObject: JsonObject) = viewModelScope.launch {
        resolveResponseStatus(null, repository.updatePushToken(jsonObject))
    }

    fun updateUserData(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        usersListener.postValue(Event(repository.updateUserData(jsonObject)))
    }

    // TODO remove try - catch
    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) = viewModelScope.launch {
        try {
            Timber.d("RoomDataCalled")
            val roomData = repository.updateRoom(jsonObject, roomId, userId).data?.room
            createRoomListener.postValue(Event(RoomCreated(roomData!!)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                createRoomListener.postValue(Event(RoomUpdateFailed))
            }
            return@launch
        }
    }

    fun unregisterSharedPrefsReceiver() = viewModelScope.launch {
        sharedPrefsRepo.unregisterSharedPrefsReceiver()
    }

    fun blockedUserListListener() = liveData {
        emitSource(sharedPrefsRepo.blockUserListener())
    }

    fun fetchBlockedUsersLocally(userIds: List<Int>) = viewModelScope.launch {
        blockedListListener.postValue(Event(repository.fetchBlockedUsersLocally(userIds)))
    }

    fun getBlockedUsersList() = viewModelScope.launch {
        repository.getBlockedList()
    }

    fun blockUser(blockedId: Int) = viewModelScope.launch {
        repository.blockUser(blockedId)
    }

    fun deleteBlock(userId: Int) = viewModelScope.launch {
        repository.deleteBlock(userId)
    }

    fun deleteBlockForSpecificUser(userId: Int) = viewModelScope.launch {
        repository.deleteBlockForSpecificUser(userId)
    }

    /**
     * This method handles mute/unmute of room depending on the data sent to it.
     *
     * @param roomId The room id to be muted in Int.
     * @param doMute Boolean which decides if the room should be muted or unmuted
     */
    fun handleRoomMute(roomId: Int, doMute: Boolean) = viewModelScope.launch {
        repository.handleRoomMute(roomId, doMute)
    }

    /**
     * This method handles pin/unpin of room depending on the data sent to it.
     *
     * @param roomId The room id to be muted in Int.
     * @param doPin Boolean which decides if the room should be pinned or unpinned
     */
    fun handleRoomPin(roomId: Int, doPin: Boolean) = viewModelScope.launch {
        repository.handleRoomPin(roomId, doPin)
    }

    fun uploadMedia(
        activity: Activity,
        uri: Uri,
        fileType: String,
        uploadPieces: Int,
        fileStream: File,
        messageBody: MessageBody?,
        isThumbnail: Boolean
    ) = viewModelScope.launch {
        try {
            uploadDownloadManager.uploadFile(
                activity,
                uri,
                fileType,
                uploadPieces,
                fileStream,
                messageBody,
                isThumbnail,
                object : FileUploadListener {
                    override fun filePieceUploaded() {
                        mediaUploadListener.postValue(Event(MediaPieceUploaded(isThumbnail)))
                    }

                    override fun fileUploadError(description: String) {
                        mediaUploadListener.postValue(Event(MediaUploadError(description)))
                    }

                    override fun fileUploadVerified(
                        path: String,
                        mimeType: String,
                        thumbId: Long,
                        fileId: Long,
                        fileType: String,
                        messageBody: MessageBody?
                    ) {
                        mediaUploadListener.postValue(
                            Event(
                                MediaUploadVerified(
                                    path,
                                    mimeType,
                                    thumbId,
                                    fileId,
                                    fileType,
                                    messageBody,
                                    isThumbnail
                                )
                            )
                        )
                    }
                })
        } catch (ex: Exception) {
            mediaUploadListener.postValue(Event(MediaUploadError(ex.message.toString())))
        }
    }

    fun getUserTheme(): Int? {
        var theme: Int? = null
        viewModelScope.launch {
            theme = sharedPrefsRepo.readUserTheme()
        }
        return theme
    }

    fun writeUserTheme(userTheme: Int) = viewModelScope.launch {
        sharedPrefsRepo.writeUserTheme(userTheme)
    }
}

sealed class MainStates
class RoomCreated(val roomData: ChatRoom) : MainStates()
class RoomUpdated(val roomData: ChatRoom) : MainStates()
object RoomCreateFailed : MainStates()
object RoomUpdateFailed : MainStates()
object UserUpdated : MainStates()
object UserUpdateFailed : MainStates()
class RoomNotificationData(
    val response: Resource<RoomWithUsers>,
    val message: Message
) : MainStates()
