package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivateGroupChats(
    val userId: Int,
    val roomId: Int?,
    val userName: String?,
    val userPhoneName: String?,
    val roomName: String?,
    val avatarId: Long,
    val phoneNumber: String?,
    var isForwarded: Boolean,
    var selected: Boolean,
    var isBot: Boolean,
) : Parcelable
