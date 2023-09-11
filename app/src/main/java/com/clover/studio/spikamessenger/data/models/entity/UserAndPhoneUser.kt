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
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserAndPhoneUser
        return user.id == other.user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}
