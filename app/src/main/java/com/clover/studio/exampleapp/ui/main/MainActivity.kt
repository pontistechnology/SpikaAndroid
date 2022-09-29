package com.clover.studio.exampleapp.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.ActivityMainBinding
import com.clover.studio.exampleapp.ui.onboarding.startOnboardingActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingSetup: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        initializeObservers()
        sendPushTokenToServer()

//        viewModel.getRoomsRemote()
    }

    private fun initializeObservers() {
        viewModel.getPushNotificationStream().asLiveData(Dispatchers.IO).observe(this) {
            Timber.d("Message $it")
        }

        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
                viewModel.setTokenExpiredFalse()
                DialogError.getInstance(this,
                    getString(R.string.warning),
                    getString(R.string.session_expired),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            // ignore
                        }

                        override fun onSecondOptionClicked() {
                            startOnboardingActivity(this@MainActivity, false)
                        }
                    })
            }
        })

//        viewModel.roomsListener.observe(this, EventObserver {
//            when (it) {
//                is RoomsFetched -> viewModel.getMessagesRemote()
//                is RoomFetchFail -> Timber.d("Failed to fetch rooms")
//                else -> Timber.d("Other error")
//            }
//        })
//
//        viewModel.messagesListener.observe(this, EventObserver {
//            when (it) {
//                is MessagesFetched -> viewModel.getMessageRecords()
//                is MessagesFetchFail -> Timber.d("Failed to fetch messages")
//                else -> Timber.d("Other error")
//            }
//        })
    }

    private fun sendPushTokenToServer() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            val jsonObject = JsonObject()

            jsonObject.addProperty(Const.JsonFields.PUSH_TOKEN, token)

            viewModel.updatePushToken(jsonObject)
            Timber.d("Token: $token")
        })
    }
}