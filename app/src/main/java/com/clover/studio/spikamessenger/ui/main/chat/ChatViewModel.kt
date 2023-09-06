package com.clover.studio.spikamessenger.ui.main.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.spikamessenger.BaseViewModel
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.models.networking.NewNote
import com.clover.studio.spikamessenger.data.models.networking.responses.MessageResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.NotesResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.RoomResponse
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.MainRepositoryImpl
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryImpl,
    private val mainRepository: MainRepositoryImpl,
    sseManager: SSEManager,
    private val uploadDownloadManager: UploadDownloadManager
) : BaseViewModel(), SSEListener {
    val messageSendListener = MutableLiveData<Event<Resource<MessageResponse?>>>()
    val roomDataListener = MutableLiveData<Event<Resource<RoomAndMessageAndRecords?>>>()
    val roomWithUsersListener = MutableLiveData<Event<Resource<RoomWithUsers?>>>()
    val fileUploadListener = MutableLiveData<Event<Resource<FileUploadVerified?>>>()
    val noteCreationListener = MutableLiveData<Event<Resource<NotesResponse?>>>()
    val noteDeletionListener = MutableLiveData<Event<NoteDeletion>>()
    val blockedListListener = MutableLiveData<Event<Resource<List<User>?>>>()
    val roomInfoUpdated = MutableLiveData<Event<Resource<RoomResponse?>>>()
    private val liveDataLimit = MutableLiveData(20)
    val messagesReceived = MutableLiveData<List<Message>>()
    val searchMessageId = MutableLiveData(0)
    var roomWithUsers = MutableLiveData<RoomWithUsers>()

    init {
        sseManager.setupListener(this)
    }

    private fun updateCounterLimit() {
        val currentLimit = liveDataLimit.value ?: 0
        Timber.d("Current limit = $currentLimit")

        liveDataLimit.postValue(currentLimit + 1)
    }

    fun storeMessageLocally(message: Message) = CoroutineScope(Dispatchers.IO).launch {
        repository.storeMessageLocally(message)

        updateCounterLimit()
    }

    override fun newMessageReceived(message: Message) {
        viewModelScope.launch {
            updateCounterLimit()
            val currentMessages = messagesReceived.value?.toMutableList() ?: mutableListOf()
            val isMessageNew = currentMessages.none { it.id == message.id }

            if (isMessageNew) {
                currentMessages.add(message)
                messagesReceived.value = currentMessages
            }
            Timber.d("Messages received: $messagesReceived")
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            messagesReceived.value = emptyList()
            Timber.d("Messages received cleared: ${messagesReceived.value}")
        }
    }

    fun sendMessage(jsonObject: JsonObject, localId: String) = viewModelScope.launch {
        val response = repository.sendMessage(jsonObject)
        if (response.status == Resource.Status.SUCCESS) {
            resolveResponseStatus(messageSendListener, response)
        } else {
            updateMessages(
                Resource.Status.ERROR.toString(),
                localId
            )
        }
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefs.readUserId()
        }

        return userId
    }

    fun deleteLocalMessages(messages: List<Message>) = viewModelScope.launch {
        repository.deleteLocalMessages(messages)
    }

    fun deleteLocalMessage(message: Message) = viewModelScope.launch {
        repository.deleteLocalMessage(message)
    }

    fun sendMessagesSeen(roomId: Int) = viewModelScope.launch {
        repository.sendMessagesSeen(roomId)
    }

    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) =
        CoroutineScope(Dispatchers.IO).launch {
            resolveResponseStatus(
                roomInfoUpdated,
                repository.updateRoom(jsonObject, roomId, userId)
            )
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

    fun getRoomAndUsers(roomId: Int) = repository.getRoomWithUsersLiveData(roomId)

    fun getRoomUsers(roomId: Int) =  repository.getRoomUsers(roomId)

//    fun getPushNotificationStream(listener: SSEListener): Flow<Message> = flow {
//        viewModelScope.launch {
//            try {
//                sseManager.startSSEStream(listener)
//            } catch (ex: Exception) {
//                Tools.checkError(ex)
//                return@launch
//            }
//        }
//    }

    fun getMessageAndRecords(roomId: Int) = Transformations.switchMap(liveDataLimit) {
        Timber.d("Limit check ${liveDataLimit.value}")
        repository.getMessagesAndRecords(roomId, it, 0)
    }

    fun fetchNextSet(roomId: Int) {
        val currentLimit = liveDataLimit.value ?: 0
        Timber.d("Current limit = $currentLimit")

        if (getMessageCount(roomId = roomId) > currentLimit)
            liveDataLimit.value = currentLimit + 20
    }

    private fun getMessageCount(roomId: Int): Int {
        var messageCount: Int

        runBlocking {
            messageCount = repository.getMessageCount(roomId)
        }
        Timber.d("Message count = $messageCount")
        return messageCount
    }

    fun getChatRoomAndMessageAndRecordsById(roomId: Int) =
        repository.getChatRoomAndMessageAndRecordsById(roomId)

    fun getSingleRoomData(roomId: Int) = viewModelScope.launch {
        resolveResponseStatus(roomDataListener, repository.getSingleRoomData(roomId))
    }

    fun getRoomWithUsers(roomId: Int) = viewModelScope.launch {
        resolveResponseStatus(roomWithUsersListener, repository.getRoomWithUsers(roomId))
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

    fun sendReaction(jsonObject: JsonObject) = viewModelScope.launch {
        repository.sendReaction(jsonObject)
    }

    fun deleteRoom(roomId: Int) = viewModelScope.launch {
        repository.deleteRoom(roomId)
    }

    fun leaveRoom(roomId: Int) = viewModelScope.launch {
        repository.leaveRoom(roomId)
    }

    fun removeAdmin(roomId: Int, userId: Int) = viewModelScope.launch {
        repository.removeAdmin(roomId, userId)
    }

    fun deleteMessage(messageId: Int, target: String) = CoroutineScope(Dispatchers.IO).launch {
        repository.deleteMessage(messageId, target)
    }

    fun updateMessages(messageStatus: String, localId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateMessageStatus(messageStatus, localId)
        }
    }

    fun updateLocalUri(localId: String, uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateLocalUri(localId, uri)
        }
    }

    fun editMessage(messageId: Int, jsonObject: JsonObject) =
        CoroutineScope(Dispatchers.IO).launch {
            repository.editMessage(messageId, jsonObject)
        }

    fun fetchNotes(roomId: Int) =
        CoroutineScope(Dispatchers.IO).launch {
            repository.getNotes(roomId)
        }

    fun getRoomNotes(roomId: Int) = repository.getLocalNotes(roomId)

    fun createNewNote(roomId: Int, newNote: NewNote) = CoroutineScope(Dispatchers.IO).launch {
        resolveResponseStatus(noteCreationListener, repository.createNewNote(roomId, newNote))
    }

    fun updateNote(noteId: Int, newNote: NewNote) = CoroutineScope(Dispatchers.IO).launch {
        resolveResponseStatus(noteCreationListener, repository.updateNote(noteId, newNote))
    }

    fun deleteNote(noteId: Int) = viewModelScope.launch {
        noteDeletionListener.postValue(Event(NoteDeletion(repository.deleteNote(noteId))))
    }

    fun unregisterSharedPrefsReceiver() = viewModelScope.launch {
        sharedPrefs.unregisterSharedPrefsReceiver()
    }

    fun blockedUserListListener() = liveData {
        emitSource(sharedPrefs.blockUserListener())
    }

    fun fetchBlockedUsersLocally(userIds: List<Int>) = viewModelScope.launch {
        resolveResponseStatus(blockedListListener, mainRepository.fetchBlockedUsersLocally(userIds))
    }

    fun getBlockedUsersList() = viewModelScope.launch {
        mainRepository.getBlockedList()
    }

// Block methods
//    fun blockUser(blockedId: Int) = viewModelScope.launch {
//        mainRepository.blockUser(blockedId)
//    }
//
//    fun deleteBlock(userId: Int) = viewModelScope.launch {
//        mainRepository.deleteBlock(userId)
//    }

    fun deleteBlockForSpecificUser(userId: Int) = viewModelScope.launch {
        resolveResponseStatus(
            blockedListListener,
            mainRepository.deleteBlockForSpecificUser(userId)
        )
    }

    fun getUnreadCount() = viewModelScope.launch {
        mainRepository.getUnreadCount()
    }

    fun updateUnreadCount(roomId: Int) = viewModelScope.launch {
        mainRepository.updateUnreadCount(roomId)
    }


    fun cancelUploadFile(messageId: String) = viewModelScope.launch {
        mainRepository.cancelUpload(messageId)
    }

    fun uploadFile(
        fileData: FileData
    ) =
        viewModelScope.launch {
            try {
                uploadDownloadManager.uploadFile(
                    fileData,
                    object : FileUploadListener {
                        override fun filePieceUploaded() {
                            resolveResponseStatus(
                                fileUploadListener,
                                Resource(Resource.Status.LOADING, null, "")
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
                            val response =
                                FileUploadVerified(
                                    path,
                                    mimeType,
                                    thumbId,
                                    fileId,
                                    fileType,
                                    messageBody,
                                    false
                                )
                            resolveResponseStatus(
                                fileUploadListener,
                                Resource(Resource.Status.SUCCESS, response, "")
                            )
                        }

                        override fun fileCanceledListener(messageId: String?) {
                            // Ignore
                        }
                    })
            } catch (ex: Exception) {
                resolveResponseStatus(
                    fileUploadListener,
                    Resource(Resource.Status.ERROR, null, ex.message.toString())
                )
            }
        }
}

class RoomNotificationData(val response: Resource<RoomWithUsers>, val message: Message)
class NoteDeletion(val response: Resource<NotesResponse>)
class FileUploadVerified(
    val path: String,
    val mimeType: String,
    val thumbId: Long,
    val fileId: Long,
    val fileType: String,
    val messageBody: MessageBody?,
    val isThumbnail: Boolean
)
