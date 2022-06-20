package com.clover.studio.exampleapp.ui.main.chat_details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.exampleapp.databinding.ActivityChatDetailsBinding
import com.clover.studio.exampleapp.utils.Const
import dagger.hilt.android.AndroidEntryPoint

fun startChatDetailsActivity(fromActivity: Activity, roomWithUsers: String, isAdmin: Boolean) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatDetailsActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomWithUsers)
        intent.putExtra(Const.Navigation.IS_ADMIN, isAdmin)
        startActivity(intent)
    }

@AndroidEntryPoint
class ChatDetailsActivity : AppCompatActivity() {

    private lateinit var bindingSetup: ActivityChatDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingSetup = ActivityChatDetailsBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)
    }
}