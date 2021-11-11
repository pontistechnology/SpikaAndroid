package com.clover.studio.exampleapp.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Const.PrefsData.Companion.SHARED_PREFS_NAME

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

    private fun getPrefs(): SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
}

interface SharedPreferencesRepository {
    suspend fun writeToken(token: String)
    suspend fun readToken(): String?
}