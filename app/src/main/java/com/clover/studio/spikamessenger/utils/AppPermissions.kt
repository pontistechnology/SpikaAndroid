package com.clover.studio.spikamessenger.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.clover.studio.spikamessenger.MainApplication

object AppPermissions {
    fun requestPermissions(activity: Activity): List<String> {
        val permissions = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        return permissions
    }

    val notificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else ""

    fun hasPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                MainApplication.appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    val hasStoragePermission: Boolean =
        ContextCompat.checkSelfPermission(
            MainApplication.appContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    const val contactsPermission: String = Manifest.permission.READ_CONTACTS
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            MainApplication.appContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
