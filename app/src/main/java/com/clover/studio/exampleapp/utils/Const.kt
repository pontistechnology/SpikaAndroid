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
            const val ROOM_ID = "room_id"
            const val IS_ADMIN = "is_admin"
            const val USER_IDS = "user_ids"
            const val USER_NUMBER = "user_number"
        }
    }

    class UserActions {
        companion object {
            const val DELETE_MESSAGE_ME = "user"
            const val DELETE_MESSAGE_ALL = "all"
            const val DOWNLOAD_FILE = "download_file"
            const val MESSAGE_ACTION = "action"
        }
    }

    class IntentExtras {
        companion object {
            const val ROOM_ID_EXTRA = "room_id_extra"
        }
    }

    class FileExtensions {
        companion object {
            const val PDF = "pdf"
            const val ZIP = "zip"
            const val RAR = "rar"
            const val MP3 = "mp3"
            const val WAW = "waw"
            const val MP4 = "mp4"
            const val MKV = "mkv"
            const val JPEG = "jpeg"
            const val PNG = "png"
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
            const val UPDATE_MESSAGE = "UPDATE_MESSAGE"
            const val DELETE_MESSAGE = "DELETE_MESSAGE"
            const val NEW_MESSAGE_RECORD = "NEW_MESSAGE_RECORD"
            const val DELETE_MESSAGE_RECORD = "DELETE_MESSAGE_RECORD"
            const val NEW_ROOM = "NEW_ROOM"
            const val UPDATE_ROOM = "UPDATE_ROOM"
            const val DELETE_ROOM = "DELETE_ROOM"
            const val USER_UPDATE = "USER_UPDATE"

            // Mime types
            const val IMAGE = "image/*"
            const val FILE = "*/*"

            // File types
            const val AVATAR = "avatar"
            const val FILE_TYPE = "file"
            const val AUDIO = "audio"
            const val VIDEO = "video"
            const val TEXT = "text"
            const val CHAT_IMAGE = "image"

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
            const val FILE_ID = "fileId"
            const val THUMB_ID = "thumbId"

            // Chat
            const val ROOM_ID = "roomId"
            const val NAME = "name"
            const val AVATAR_URL = "avatarUrl"
            const val USER_IDS = "userIds"
            const val ADMIN_USER_IDS = "adminUserIds"
            const val PRIVATE = "private"
            const val GROUP = "group"
            const val BODY = "body"
            const val MESSAGE_IDS = "messagesIds"
            const val REACTION = "reaction"
        }
    }

    class Networking {
        companion object {
            // Sync Data
            const val API_SYNC_MESSAGES = "api/messenger/messages/sync/{lastUpdate}"
            const val API_SYNC_MESSAGE_RECORDS = "api/messenger/message-records/sync/{lastUpdate}"
            const val API_POST_REACTION = "api/messenger/message-records"
            const val API_DELETE_REACTION = "api/messenger/message-records/{id}"
            const val API_SYNC_USERS = "api/messenger/users/sync/{lastUpdate}"
            const val API_SYNC_ROOMS = "api/messenger/rooms/sync/{lastUpdate}"

            const val API_AUTH = "api/messenger/auth"
            const val API_VERIFY_CODE = "api/messenger/auth/verify"
            const val API_CONTACTS = "api/messenger/contacts"  // Fix
            const val API_UPDATE_USER = "api/messenger/me" // Fix
            const val API_UPLOAD_FILE = "api/upload/files" // Fix
            const val API_VERIFY_FILE = "api/upload/files/verify" // Fix
            const val API_POST_MESSAGE = "api/messenger/messages"
            const val API_GET_MESSAGES = "api/messenger/messages/roomId/{roomId}" // Fix
            const val API_GET_MESSAGES_TIMESTAMP = "api/messenger/messages"
            const val API_MESSAGE_DELIVERED = "api/messenger/messages/delivered" // Fix
            const val API_GET_ROOM_BY_ID = "api/messenger/rooms/users/{userId}"
            const val API_POST_NEW_ROOM = "api/messenger/rooms" // Fix
            const val API_SSE_STREAM = "api/sse"
            const val API_UPDATE_TOKEN = "api/messenger/device"
            const val API_GET_MESSAGE_RECORDS = "api/messenger/messages/{messageId}/message-records"
            const val API_MESSAGES_SEEN = "api/messenger/messages/{roomId}/seen"
            const val API_UPDATE_ROOM = "api/messenger/rooms/{roomId}" // Fix
            const val API_MUTE_ROOM = "api/messenger/rooms/{roomId}/mute"
            const val API_UNMUTE_ROOM = "api/messenger/rooms/{roomId}/unmute"
            const val API_GET_SETTINGS = "api/messenger/me/settings"
            const val API_UPDATE_MESSAGE = "api/messenger/messages/{id}"

            // Queries
            const val TIMESTAMP = "timestamp"
            const val ROOM_ID = "roomId"
            const val CONTACTS = "contacts"
            const val USER_ID = "userId"
            const val DEVICE_ID = "deviceId"
            const val MESSAGE_ID = "messageId"
            const val LAST_UPDATE = "lastUpdate"
            const val PAGE = "page"
            const val ID = "id"
            const val TARGET = "target"
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
            const val FIRST_START = "firstStart"
            const val PHONE_NUMBER = "phoneNumber"
            const val DEVICE_ID = "deviceId"
            const val COUNTRY_CODE = "countryCode"
            const val REGISTERED = "registered"

            // Sync
            const val MESSAGE_RECORD_SYNC = "message_record_sync"
            const val MESSAGE_SYNC = "message_sync"
            const val USER_SYNC = "user_sync"
            const val ROOM_SYNC = "room_sync"
        }
    }

    class UserData {
        companion object {
            const val DISPLAY_NAME: String = "displayName"
            const val AVATAR_URL: String = "avatarUrl"
        }
    }
}