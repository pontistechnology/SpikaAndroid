package com.clover.studio.exampleapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import timber.log.Timber


class SmsReceiver : BroadcastReceiver() {
    private var mListener: SmsListener? = null

    fun bindListener(listener: SmsListener) {
        mListener = listener
    }

    override fun onReceive(context: Context?, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            val status = extras!![SmsRetriever.EXTRA_STATUS] as Status?
            when (status!!.statusCode) {
                CommonStatusCodes.SUCCESS -> { // Get SMS message contents
                    val message: String? = extras[SmsRetriever.EXTRA_SMS_MESSAGE] as String?
                    mListener?.messageReceived(message)
                    Timber.d("MESSAGE $message")
                }
                CommonStatusCodes.TIMEOUT -> {
                    Timber.d("MESSAGE TIMEOUT")
                }
            }
        }
    }
}

interface SmsListener {
    fun messageReceived(messageText: String?)
}