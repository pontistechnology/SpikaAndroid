package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivateGroupChats(
    val private: UserAndPhoneUser?,
    val group: RoomWithUsers?
) : Parcelable
