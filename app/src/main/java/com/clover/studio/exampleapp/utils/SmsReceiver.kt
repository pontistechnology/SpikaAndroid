package com.clover.studio.exampleapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage


object SmsReceiver : BroadcastReceiver() {
    private var mListener: SmsListener? = null
    var formattedBody: String? = null

    fun bindListener(listener: SmsListener) {
        mListener = listener
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data: Bundle = p1?.extras!!
        val pdus = data["pdus"] as Array<*>?

        if (pdus != null) {
            for (char in pdus) {
                val smsMessage: SmsMessage =
                    SmsMessage.createFromPdu(pdus[char as Int] as ByteArray?)
                val sender: String = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                formattedBody = messageBody.replace("[^0-9]".toRegex(), "")
                //Pass on the text to our listener.
                if (formattedBody != null) {
                    mListener?.messageReceived(formattedBody) // attach value to interface
                } else {
                }
            }
        }
    }
}

interface SmsListener {
    fun messageReceived(messageText: String?)
}