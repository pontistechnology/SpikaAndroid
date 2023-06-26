package com.clover.studio.spikamessenger.utils

import android.Manifest
import android.os.Build

val permissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CONTACTS)
    } else {
        listOf(Manifest.permission.READ_CONTACTS)
    }

val notificationPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else ""