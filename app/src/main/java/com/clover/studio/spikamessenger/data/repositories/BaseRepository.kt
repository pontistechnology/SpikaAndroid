package com.clover.studio.spikamessenger.data.repositories

import com.clover.studio.exampleapp.MainApplication
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.networking.responses.ContactsSyncResponse
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.helpers.BaseDataSource
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.clover.studio.exampleapp.utils.helpers.RestOperations

interface BaseRepository {
    suspend fun syncContacts(
        userDao: UserDao,
        shouldRefresh: Boolean,
        sharedPrefs: SharedPreferencesRepository,
        baseDataSource: BaseDataSource
    ): Resource<ContactsSyncResponse> {
        if (!sharedPrefs.isTeamMode()) {
            if (shouldRefresh || sharedPrefs.readContactSyncTimestamp() == 0L || System.currentTimeMillis() < (sharedPrefs.readContactSyncTimestamp() + Const.Time.DAY)) {
                val contacts = Tools.fetchPhonebookContacts(
                    MainApplication.appContext,
                    sharedPrefs.readCountryCode()
                )
                return if (contacts != null) {
                    val phoneNumbersHashed = Tools.getContactsNumbersHashed(
                        MainApplication.appContext,
                        sharedPrefs.readCountryCode(),
                        contacts
                    ).toList()

                    // Beginning offset is always 0
                    syncNextBatch(userDao, phoneNumbersHashed, 0, ArrayList(), baseDataSource)
                } else Resource<ContactsSyncResponse>(
                    Resource.Status.ERROR,
                    null,
                    "Contacts are empty"
                )
            } else {
                return Resource<ContactsSyncResponse>(
                    Resource.Status.ERROR,
                    null,
                    "Not enough time passed for sync"
                )
            }
        } else {
            return Resource<ContactsSyncResponse>(
                Resource.Status.SUCCESS,
                null,
                "App is in team mode"
            )
        }
    }

    private suspend fun syncNextBatch(
        userDao: UserDao,
        contacts: List<String>,
        offset: Int,
        contactList: MutableList<User>,
        baseDataSource: BaseDataSource
    ): Resource<ContactsSyncResponse> {
        val endIndex = (offset + CONTACTS_BATCH).coerceAtMost(contacts.size)
        val batchedList = contacts.subList(offset, endIndex)


        val isLastPage = offset + CONTACTS_BATCH > contacts.size

        val response = RestOperations.performRestOperation(
            networkCall = {
                baseDataSource.syncContacts(
                    contacts = batchedList,
                    isLastPage = isLastPage
                )
            },
            saveCallResult = {
                if (isLastPage) {
                    it.data?.list?.let { users -> contactList.addAll(users) }
                    userDao.upsert(contactList)
                    contactList.map { user -> user.id }
                        .let { users -> userDao.removeSpecificUsers(users) }
                } else {
                    it.data?.list?.let { users -> contactList.addAll(users) }
                }
            }
        )

        if (Resource.Status.SUCCESS == response.status) {
            if (!isLastPage) {
                syncNextBatch(
                    userDao,
                    contacts,
                    offset + CONTACTS_BATCH,
                    contactList,
                    baseDataSource
                )
            } else return response
        } else return response

        return response
    }
}