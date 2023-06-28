package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserAndPhoneUser(
    @Embedded val user: User,
    @Relation(parentColumn = "telephone_number", entityColumn = "number")
    val phoneUser: PhoneUser?
) : Parcelable