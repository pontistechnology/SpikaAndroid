package com.clover.studio.spikamessenger.data.repositories.data_sources

import com.clover.studio.spikamessenger.data.models.networking.NewNote
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.services.ChatService
import com.clover.studio.spikamessenger.utils.Tools.getHeaderMap
import com.clover.studio.spikamessenger.utils.helpers.BaseDataSource
import com.google.gson.JsonObject
import javax.inject.Inject

class ChatRemoteDataSource @Inject constructor(
    private val retrofitService: ChatService,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseDataSource() {

    suspend fun sendMessage(jsonObject: JsonObject) = getResult {
        retrofitService.sendMessage(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }

    suspend fun sendMessagesSeen(roomId: Int) = getResult {
        retrofitService.sendMessagesSeen(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun deleteMessage(messageId: Int, target: String) = getResult {
        retrofitService.deleteMessage(getHeaderMap(sharedPrefs.readToken()), messageId, target)
    }

    suspend fun editMessage(messageId: Int, jsonObject: JsonObject) = getResult {
        retrofitService.editMessage(
            getHeaderMap(sharedPrefs.readToken()),
            messageId,
            jsonObject
        )
    }

    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int) = getResult {
        retrofitService.updateRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject, roomId)
    }

    suspend fun muteRoom(roomId: Int) = getResult {
        retrofitService.muteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun unmuteRoom(roomId: Int) = getResult {
        retrofitService.unmuteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun pinRoom(roomId: Int) = getResult {
        retrofitService.pinRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun unpinRoom(roomId: Int) = getResult {
        retrofitService.unpinRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun postReaction(jsonObject: JsonObject) = getResult {
        retrofitService.postReaction(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }

    suspend fun getRoomNotes(roomId: Int) = getResult {
        retrofitService.getRoomNotes(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun createNewNote(roomId: Int, newNote: NewNote) = getResult {
        retrofitService.createNote(getHeaderMap(sharedPrefs.readToken()), roomId, newNote)
    }

    suspend fun updateNote(noteId: Int, newNote: NewNote) = getResult {
        retrofitService.updateNote(getHeaderMap(sharedPrefs.readToken()), noteId, newNote)
    }

    suspend fun deleteNote(noteId: Int) = getResult {
        retrofitService.deleteNote(getHeaderMap(sharedPrefs.readToken()), noteId)
    }

    suspend fun deleteRoom(roomId: Int) = getResult {
        retrofitService.deleteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun leaveRoom(roomId: Int) = getResult {
        retrofitService.leaveRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
    }

    suspend fun getUnreadCount() = getResult {
        retrofitService.getUnreadCount(getHeaderMap(sharedPrefs.readToken()))
    }

    override suspend fun syncContacts(
        contacts: List<String>,
        isLastPage: Boolean
    ): Resource<ContactsSyncResponse> {
        // This should never be called
        return Resource<ContactsSyncResponse>(Resource.Status.ERROR, null, null)
    }
}
