package com.clover.studio.exampleapp.utils.helpers

import androidx.room.TypeConverter
import com.clover.studio.exampleapp.data.models.MessageBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object TypeConverter {
    @TypeConverter
    fun fromString(value: String?): ArrayList<String> {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }

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
    fun stringToMap(value: String?): Map<String, String> {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String?, String?>?>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun mapToString(value: Map<String?, String?>?): String {
        val gson = Gson()
        return gson.toJson(value)
    }
}