package com.clover.studio.spikamessenger.utils.helpers

import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import timber.log.Timber

class PhonebookService : Service() {
    private lateinit var phonebookObserver: PhonebookObserver

    override fun onCreate() {
        super.onCreate()
        phonebookObserver = PhonebookObserver(Handler(Looper.getMainLooper()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        phonebookObserver.registerObserver()
        return START_STICKY
    }

    override fun onDestroy() {
        phonebookObserver.unregisterObserver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private inner class PhonebookObserver(handler: Handler) : ContentObserver(handler) {
        private val contentResolver: ContentResolver = applicationContext.contentResolver

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            // Handle the phonebook change here
            Timber.d("Phonebook changed")
        }

        fun registerObserver() {
            val contactsUri: Uri = ContactsContract.Contacts.CONTENT_URI
            contentResolver.registerContentObserver(contactsUri, true, this)
        }

        fun unregisterObserver() {
            contentResolver.unregisterContentObserver(this)
        }
    }
}
