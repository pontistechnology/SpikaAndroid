package com.clover.studio.spikamessenger.data.repositories.data_sources

import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.services.SSEService
import com.clover.studio.spikamessenger.utils.Tools.getHeaderMap
import com.clover.studio.spikamessenger.utils.helpers.BaseDataSource
import com.google.gson.JsonObject
import javax.inject.Inject

class SSERemoteDataSource @Inject constructor(
    private val retrofitService: SSEService,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseDataSource() {

    suspend fun syncMessageRecords(messageRecordsTimestamp: Long) = getResult {
        retrofitService.syncMessageRecords(
            getHeaderMap(sharedPrefs.readToken()),
            messageRecordsTimestamp
        )
    }

    suspend fun syncMessages(messageTimestamp: Long) = getResult {
        retrofitService.syncMessages(getHeaderMap(sharedPrefs.readToken()), messageTimestamp)
    }

    suspend fun syncUsers(userTimestamp: Long) = getResult {
        retrofitService.syncUsers(getHeaderMap(sharedPrefs.readToken()), userTimestamp)
    }

    suspend fun syncRooms(roomTimestamp: Long) = getResult {
        retrofitService.syncRooms(getHeaderMap(sharedPrefs.readToken()), roomTimestamp)
    }

    suspend fun sendMessageDelivered(messageId: JsonObject) = getResult {
        retrofitService.sendMessageDelivered(getHeaderMap(sharedPrefs.readToken()), messageId)
    }

    suspend fun getUnreadCount() = getResult {
        retrofitService.getUnreadCount(getHeaderMap(sharedPrefs.readToken()))
    }

    override suspend fun syncContacts(contacts: List<String>, isLastPage: Boolean) = getResult {
        retrofitService.syncContacts(getHeaderMap(sharedPrefs.readToken()), contacts, isLastPage)
    }

    suspend fun getAppMode() = getResult {
        retrofitService.getAppMode(getHeaderMap(sharedPrefs.readToken()))
    }
}
