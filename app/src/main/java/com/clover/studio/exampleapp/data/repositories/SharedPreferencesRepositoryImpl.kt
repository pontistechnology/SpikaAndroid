package com.clover.studio.exampleapp.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Const.PrefsData.Companion.SHARED_PREFS_NAME
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class SharedPreferencesRepositoryImpl(
    private val context: Context
) : SharedPreferencesRepository {
    override suspend fun writeToken(token: String) {
        with(getPrefs().edit()) {
            putString(Const.PrefsData.TOKEN, token)
            commit()
        }
    }

    override suspend fun readToken(): String? = getPrefs().getString(Const.PrefsData.TOKEN, null)

    override suspend fun writeContacts(contacts: List<String>) {
        with(getPrefs().edit()) {
            val gson = Gson()
            putString(Const.PrefsData.USER_CONTACTS, gson.toJson(contacts))
            commit()
        }
    }

    override suspend fun readContacts(): List<String>? {
        val serializedObject: String? = getPrefs().getString(Const.PrefsData.USER_CONTACTS, null)
        return if (serializedObject != null) {
            val gson = Gson()
            val type: Type = object : TypeToken<List<String?>?>() {}.type
            gson.fromJson<List<String>>(serializedObject, type)
        } else {
            null
        }
    }

    override suspend fun writeUserId(id: Int) {
        with(getPrefs().edit()) {
            putInt(Const.PrefsData.USER_ID, id)
            commit()
        }
    }

    override suspend fun readUserId(): Int = getPrefs().getInt(Const.PrefsData.USER_ID, 0)

    override suspend fun isAccountCreated(): Boolean =
        getPrefs().getBoolean(Const.PrefsData.ACCOUNT_CREATED, false)

    override suspend fun accountCreated(created: Boolean) {
        with(getPrefs().edit()) {
            putBoolean(Const.PrefsData.ACCOUNT_CREATED, created)
            commit()
        }
    }

    override suspend fun writePushToken(token: String) {
        with(getPrefs().edit()) {
            putString(Const.PrefsData.PUSH_TOKEN, token)
            commit()
        }
    }

    override suspend fun readPushToken(): String? =
        getPrefs().getString(Const.PrefsData.PUSH_TOKEN, null)

    override suspend fun isNewUser(): Boolean =
        getPrefs().getBoolean(Const.PrefsData.NEW_USER, false)

    override suspend fun setNewUser(newUser: Boolean) {
        with(getPrefs().edit()) {
            putBoolean(Const.PrefsData.NEW_USER, newUser)
            commit()
        }
    }

    override suspend fun writeFirstSSELaunch() {
        with(getPrefs().edit()) {
            putBoolean(
                Const.PrefsData.DATA_SYNCED,
                false
            )
            commit()
        }
    }

    override suspend fun isFirstSSELaunch(): Boolean =
        getPrefs().getBoolean(Const.PrefsData.DATA_SYNCED, true)

    private fun getPrefs(): SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
}

interface SharedPreferencesRepository {
    suspend fun writeToken(token: String)
    suspend fun readToken(): String?
    suspend fun writeContacts(contacts: List<String>)
    suspend fun readContacts(): List<String>?
    suspend fun writeUserId(id: Int)
    suspend fun readUserId(): Int?
    suspend fun isAccountCreated(): Boolean
    suspend fun accountCreated(created: Boolean)
    suspend fun writePushToken(token: String)
    suspend fun readPushToken(): String?
    suspend fun isNewUser(): Boolean
    suspend fun setNewUser(newUser: Boolean)
    suspend fun writeFirstSSELaunch()
    suspend fun isFirstSSELaunch(): Boolean
}