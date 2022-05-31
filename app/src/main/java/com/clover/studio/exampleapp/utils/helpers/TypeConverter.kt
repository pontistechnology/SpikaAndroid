package com.clover.studio.exampleapp.utils.helpers

import androidx.room.TypeConverter
import com.clover.studio.exampleapp.data.models.MessageBody
import com.clover.studio.exampleapp.data.models.networking.RoomUsers
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object TypeConverter {

    @TypeConverter
    fun stringToMessages(json: String?): MessageBody? {
        val gson = Gson()
        return if (json != null) {
            val type: Type = object : TypeToken<MessageBody?>() {}.type
            gson.fromJson(json, type)
        } else null
    }

    @TypeConverter
    fun messageToString(messageBody: MessageBody?): String {
        val gson = Gson()
        val type: Type = object : TypeToken<MessageBody?>() {}.type
        return gson.toJson(messageBody, type)
    }

    @TypeConverter
    fun stringToRoomUser(json: String?): ArrayList<RoomUsers> {
        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<RoomUsers>?>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun roomUserToString(roomUsers: ArrayList<RoomUsers>): String {
        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<RoomUsers>?>() {}.type
        return gson.toJson(roomUsers, type)
    }

}