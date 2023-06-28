package com.clover.studio.spikamessenger.data.repositories

import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.data.daos.UserDao
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.networking.responses.ContactsSyncResponse
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.BaseDataSource
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.RestOperations
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val CONTACTS_BATCH = 40
private val mutex = Mutex()

interface BaseRepository {
    suspend fun syncContacts(
        userDao: UserDao,
        shouldRefresh: Boolean,
        sharedPrefs: SharedPreferencesRepository,
        baseDataSource: BaseDataSource
    ): Resource<ContactsSyncResponse> {
        return mutex.withLock {
            startContactsSync(userDao, shouldRefresh, sharedPrefs, baseDataSource)
        }
    }

    private suspend fun startContactsSync(
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