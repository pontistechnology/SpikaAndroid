package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivateGroupChats(
    val name: String?,
    val formattedDisplayName: String?,
    val telephoneNumber: String,
    val avatarFileId: Long,
    val id: Int,
    val isForwarded: Boolean,
    var isSelected: Boolean,
    val isGroup: Boolean,
    val isBot: Boolean,
) : Parcelable
