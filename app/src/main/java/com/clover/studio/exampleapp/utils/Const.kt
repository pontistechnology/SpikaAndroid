package com.clover.studio.exampleapp.utils

class Const {
    class Navigation {
        companion object {
            const val COUNTRY_CODE: String = "county_code"
            const val PHONE_NUMBER: String = "phone_number"
            const val DEVICE_ID: String = "device_id"
            const val PHONE_NUMBER_HASHED = "phone_number_hashed"
        }
    }

    class Networking {
        companion object {
            const val API_AUTH = "messenger/auth"
            const val API_VERIFY_CODE = "/messenger/auth/verify"
        }
    }
}