package com.clover.studio.exampleapp.utils.helpers

import androidx.room.TypeConverter
import com.clover.studio.exampleapp.data.models.entity.MessageBody
import com.clover.studio.exampleapp.data.models.entity.RecordMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object TypeConverter {

    @TypeConverter
    fun stringToMessages(json: String?): MessageBody? {
        val gson = GsonProvider.gson
        return if (json != null) {
            val type: Type = object : TypeToken<MessageBody?>() {}.type
            gson.fromJson(json, type)
        } else null
    }

    @TypeConverter
    fun messageToString(messageBody: MessageBody?): String {
        val gson = GsonProvider.gson
        val type: Type = object : TypeToken<MessageBody?>() {}.type
        return gson.toJson(messageBody, type)
    }

    @TypeConverter
    fun stringToRecordMessages(json: String?): RecordMessage? {
        val gson = Gson()
        return if (json != null) {
            val type: Type = object : TypeToken<RecordMessage?>() {}.type
            gson.fromJson(json, type)
        } else null
    }

    @TypeConverter
    fun recordMessageToString(messageBody: RecordMessage?): String {
        val gson = Gson()
        val type: Type = object : TypeToken<RecordMessage?>() {}.type
        return gson.toJson(messageBody, type)
    }
}