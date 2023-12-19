package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivateGroupChats(
    val private: UserAndPhoneUser?,
    val group: RoomWithMessage?
) : Parcelable
