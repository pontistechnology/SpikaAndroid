package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.UploadDownloadManager
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    var roomWithUsers: RoomWithUsers ?= null

    private lateinit var bindingSetup: ActivityChatScreenBinding

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)

        val view = bindingSetup.root
        setContentView(view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_chat_container) as NavHostFragment
        val navController = navHostFragment.navController


        // Fetch room data sent from previous activity
        val gson = Gson()
        roomWithUsers = gson.fromJson(
            intent.getStringExtra(Const.Navigation.ROOM_DATA),
            RoomWithUsers::class.java
        )

        initViews()
        initializeObservers()
    }


    private fun initializeObservers() {
    }


    private fun initViews() {
    }
}