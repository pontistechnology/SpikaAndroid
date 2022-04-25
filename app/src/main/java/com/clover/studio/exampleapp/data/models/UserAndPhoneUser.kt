package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class UserAndPhoneUser(
    @Embedded val user: User,
    @Relation(parentColumn = "telephone_number", entityColumn = "number")
    val phoneUser: PhoneUser?
)