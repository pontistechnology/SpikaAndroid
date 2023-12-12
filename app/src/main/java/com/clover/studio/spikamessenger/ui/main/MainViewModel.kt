package com.clover.studio.spikamessenger.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.spikamessenger.BaseViewModel
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.MessageWithRoom
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.models.networking.responses.AuthResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.ContactsSyncResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.DeleteUserResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.RoomResponse
import com.clover.studio.spikamessenger.data.repositories.MainRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SSERepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.ui.main.chat.FileUploadVerified
import com.clover.studio.spikamessenger.utils.Event
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.SSEListener
import com.clover.studio.spikamessenger.utils.SSEManager
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository,
    private val sseManager: SSEManager,
    private val sseRepository: SSERepositoryImpl,
    private val uploadDownloadManager: UploadDownloadManager
) : BaseViewModel(), SSEListener {
    val usersListener = MutableLiveData<Event<Resource<AuthResponse?>>>()
    val checkRoomExistsListener = MutableLiveData<Event<Resource<RoomResponse?>>>()
    val createRoomListener = MutableLiveData<Event<Resource<RoomResponse?>>>()
    val roomWithUsersListener = MutableLiveData<Event<Resource<RoomWithUsers?>>>()
    val roomNotificationListener = MutableLiveData<Event<RoomNotificationData>>()
    val blockedListListener = MutableLiveData<Event<Resource<List<User>?>>>()
    val fileUploadListener = MutableLiveData<Event<Resource<FileUploadVerified?>>>()
    val newMessageReceivedListener = MutableLiveData<Event<Resource<Message?>>>()
    val contactSyncListener = MutableLiveData<Event<Resource<ContactsSyncResponse?>>>()
    val deleteUserListener = MutableLiveData<Event<Resource<DeleteUserResponse?>>>()
    val searchedMessageListener = MutableLiveData<Event<Resource<List<MessageWithRoom>?>>>()
    val roomUsers: MutableList<UserAndPhoneUser> = ArrayList()

    init {
        sseManager.setupListener(this)
    }

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

    override fun newMessageReceived(message: Message) {
        resolveResponseStatus(
            newMessageReceivedListener,
            Resource(Resource.Status.SUCCESS, message, "")
        )
    }

    fun saveSelectedUsers(users: MutableList<UserAndPhoneUser>) {
        roomUsers.addAll(users.toMutableSet())
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
    }

    fun createNewRoom(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        resolveResponseStatus(createRoomListener, repository.createNewRoom(jsonObject))
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

    fun getSearchedMessages(query: String) = viewModelScope.launch {
        resolveResponseStatus(searchedMessageListener, repository.getSearchedMessages(query))
    }

    fun getUserAndPhoneUser(localId: Int) = repository.getUserAndPhoneUser(localId)

    fun getChatRoomsWithLatestMessage() = repository.getChatRoomsWithLatestMessage()

    fun getRecentMessages(chatType: String) = repository.getRecentMessages(chatType = chatType)

    fun getRoomsLiveData() = repository.getRoomsUnreadCount()

    fun getRoomByIdLiveData(roomId: Int) = repository.getRoomByIdLiveData(roomId)

    fun getRoomWithUsers(roomId: Int) =
        viewModelScope.launch {
            resolveResponseStatus(roomWithUsersListener, repository.getRoomWithUsers(roomId))
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

    fun getUnreadCount() = viewModelScope.launch {
        repository.getUnreadCount()
    }

    fun updatePushToken(jsonObject: JsonObject) = viewModelScope.launch {
        resolveResponseStatus(null, repository.updatePushToken(jsonObject))
    }

    fun updateUserData(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        resolveResponseStatus(usersListener, repository.updateUserData(jsonObject))
    }

    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) =
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("RoomDataCalled")
            resolveResponseStatus(
                createRoomListener,
                repository.updateRoom(jsonObject, roomId, userId)
            )
        }

    fun unregisterSharedPrefsReceiver() = viewModelScope.launch {
        sharedPrefsRepo.unregisterSharedPrefsReceiver()
    }

    fun blockedUserListListener() = liveData {
        emitSource(sharedPrefsRepo.blockUserListener())
    }

    fun fetchBlockedUsersLocally(userIds: List<Int>) = viewModelScope.launch {
        resolveResponseStatus(blockedListListener, repository.fetchBlockedUsersLocally(userIds))
    }

    fun getBlockedUsersList() = viewModelScope.launch {
        repository.getBlockedList()
    }

    fun blockUser(blockedId: Int) = viewModelScope.launch {
        repository.blockUser(blockedId)
    }

    /* Delete block method - uncomment when we need it
    fun deleteBlock(userId: Int) = viewModelScope.launch {
        repository.deleteBlock(userId)
    } */

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
        fileData: FileData
    ) = viewModelScope.launch {
        try {
            uploadDownloadManager.uploadFile(
                fileData,
                object : FileUploadListener {
                    override fun filePieceUploaded() {
                        resolveResponseStatus(
                            fileUploadListener,
                            Resource(Resource.Status.LOADING, null, fileData.isThumbnail.toString())
                        )
                    }

                    override fun fileUploadError(description: String) {
                        resolveResponseStatus(
                            fileUploadListener,
                            Resource(Resource.Status.ERROR, null, description)
                        )
                    }

                    override fun fileUploadVerified(
                        path: String,
                        mimeType: String,
                        thumbId: Long,
                        fileId: Long,
                        fileType: String,
                        messageBody: MessageBody?
                    ) {
                        val response = FileUploadVerified(
                            path,
                            mimeType,
                            thumbId,
                            fileId,
                            messageBody,
                        )
                        resolveResponseStatus(
                            fileUploadListener,
                            Resource(Resource.Status.SUCCESS, response, "")
                        )
                    }

                    override fun fileCanceledListener(messageId: String?) {

                    }
                })
        } catch (ex: Exception) {
            resolveResponseStatus(
                fileUploadListener,
                Resource(Resource.Status.ERROR, null, ex.message.toString())
            )
        }
    }

    fun writeUserTheme(userTheme: String) = viewModelScope.launch {
        sharedPrefsRepo.writeUserTheme(userTheme)
    }

    fun syncContacts() = CoroutineScope(Dispatchers.IO).launch {
        if (sharedPrefsRepo.isTeamMode()) {
            sseRepository.syncUsers()
            contactSyncListener.postValue(Event(Resource(Resource.Status.SUCCESS, null, null)))
        } else {
            resolveResponseStatus(
                contactSyncListener,
                repository.syncContacts(shouldRefresh = true)
            )
        }
    }

    fun deleteUser() = viewModelScope.launch {
        resolveResponseStatus(deleteUserListener, repository.deleteUser())
    }

    fun isInTeamMode() = viewModelScope.launch {
        if (sharedPrefsRepo.isTeamMode()) {
            Timber.d("App is in team mode!")
        } else Timber.d("App is in messenger mode!")
    }
}

class RoomNotificationData(
    val response: Resource<RoomWithUsers>,
    val message: Message
)
