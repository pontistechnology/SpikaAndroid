package com.clover.studio.exampleapp.utils

class Const {
    class Navigation {
        companion object {
            const val COUNTRY_CODE: String = "county_code"
            const val PHONE_NUMBER: String = "phone_number"
            const val DEVICE_ID: String = "device_id"
            const val PHONE_NUMBER_HASHED = "phone_number_hashed"
            const val USER_PROFILE = "user_profile"
            const val ROOM_DATA = "room_data"
            const val GO_ACCOUNT_CREATION = "go_account_creation"
            const val SELECTED_USERS = "selected_users"
        }
    }

    class JsonFields {
        companion object {
            const val CODE: String = "code"
            const val DEVICE_ID = "deviceId"
            const val TELEPHONE_NUMBER = "telephoneNumber"
            const val TELEPHONE_NUMBER_HASHED = "telephoneNumberHashed"
            const val COUNTRY_CODE = "countryCode"
            const val PUSH_TOKEN = "pushToken"

            // SSE
            const val NEW_MESSAGE = "NEW_MESSAGE"
            const val NEW_MESSAGE_RECORD = "NEW_MESSAGE_RECORD"
            const val DELETED_MESSAGE_RECORD = "DELETED_MESSAGE_RECORD"
            const val NEW_ROOM = "NEW_ROOM"
            const val UPDATE_ROOM = "UPDATE_ROOM"
            const val DELETE_ROOM = "DELETE_ROOM"
            const val USER_UPDATE = "USER_UPDATE"

            // Mime types
            const val IMAGE = "image/*"

            // File types
            const val AVATAR = "avatar"

            // File upload
            const val CHUNK = "chunk"
            const val OFFSET = "offset"
            const val TOTAL = "total"
            const val SIZE = "size"
            const val MIME_TYPE = "mimeType"
            const val FILE_NAME = "fileName"
            const val TYPE = "type"
            const val FILE_HASH = "fileHash"
            const val RELATION_ID = "relationId"
            const val CLIENT_ID = "clientId"

            // Chat
            const val ROOM_ID = "roomId"
            const val TEXT = "text"
            const val NAME = "name"
            const val AVATAR_URL = "avatarUrl"
            const val USER_IDS = "userIds"
            const val ADMIN_USER_IDS = "adminUserIds"
            const val PRIVATE = "private"
            const val GROUP = "group"
            const val BODY = "body"
            const val MESSAGE_IDS = "messagesIds"
        }
    }

    class Networking {
        companion object {
            // Sync Data
            const val API_SYNC_MESSAGES = "api/messenger/messages/sync"
            const val API_SYNC_MESSAGE_RECORDS = "api/messenger/message-records/sync/{lastUpdate}"
            const val API_SYNC_USERS = "api/messenger/users/sync/{lastUpdate}"
            const val API_SYNC_ROOMS = "api/messenger/rooms/sync/{lastUpdate}"

            const val API_AUTH = "api/messenger/auth"
            const val API_VERIFY_CODE = "api/messenger/auth/verify"
            const val API_CONTACTS = "api/messenger/contacts"
            const val API_UPDATE_USER = "api/messenger/me"
            const val API_UPLOAD_FILE = "api/upload/files"
            const val API_VERIFY_FILE = "api/upload/files/verify"
            const val API_POST_MESSAGE= "api/messenger/messages"
            const val API_GET_MESSAGES = "api/messenger/messages/roomId/{roomId}"
            const val API_GET_MESSAGES_TIMESTAMP = "api/messenger/messages"
            const val API_MESSAGE_DELIVERED = "api/messenger/messages/delivered"
            const val API_GET_ROOM_BY_ID = "api/messenger/rooms/users/{userId}"
            const val API_POST_NEW_ROOM = "api/messenger/rooms"
            const val API_SSE_STREAM = "api/sse"
            const val API_UPDATE_TOKEN = "api/messenger/device"
            const val API_GET_MESSAGE_RECORDS = "api/messenger/messages/{messageId}/message-records"
            const val API_MESSAGES_SEEN = "api/messenger/messages/{roomId}/seen"

            // Queries
            const val TIMESTAMP = "timestamp"
            const val ROOM_ID = "roomId"
            const val CONTACTS = "contacts"
            const val USER_ID = "userId"
            const val DEVICE_ID = "deviceId"
            const val MESSAGE_ID = "messageId"
            const val LAST_UPDATE = "lastUpdate"
            const val PAGE = "page"
        }
    }

    class Headers {
        companion object {
            const val ACCESS_TOKEN = "accesstoken"
            const val OS_NAME = "os-name"
            const val OS_VERSION = "os-version"
            const val DEVICE_NAME = "device-name"
            const val APP_VERSION = "app-version"
            const val LANGUAGE = "lang"

            // Field values
            const val ANDROID = "android"
        }
    }

    class PrefsData {
        companion object {
            const val SHARED_PREFS_NAME = "app_general_prefs"
            const val TOKEN = "token"
            const val USER_CONTACTS = "user_contacts"
            const val USER_ID = "user_id"
            const val ACCOUNT_CREATED = "account_created"
            const val PUSH_TOKEN = "pushToken"
            const val NEW_USER = "newUser"
            const val DATA_SYNCED = "dataSynced"
        }
    }

    class UserData {
        companion object {
            const val DISPLAY_NAME: String = "displayName"
            const val AVATAR_URL: String = "avatarUrl"
        }
    }
}