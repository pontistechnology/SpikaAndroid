package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.NavHostFragment
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.ui.main.SingleRoomData
import com.clover.studio.exampleapp.ui.main.SingleRoomFetchFailed
import com.clover.studio.exampleapp.ui.onboarding.startOnboardingActivity
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject


fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }

fun replaceChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
        finish()
    }

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    var roomWithUsers: RoomWithUsers? = null

    private lateinit var bindingSetup: ActivityChatScreenBinding
    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)

        val view = bindingSetup.root
        setContentView(view)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_chat_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Fetch room data sent from previous activity
        val gson = Gson()
        roomWithUsers = gson.fromJson(
            intent.getStringExtra(Const.Navigation.ROOM_DATA),
            RoomWithUsers::class.java
        )

        Timber.d("chatScreen ${roomWithUsers.toString()}")
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
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
                            viewModel.setTokenExpiredFalse()
                            startOnboardingActivity(this@ChatScreenActivity, false)
                        }
                    })
            }
        })

        viewModel.roomDataListener.observe(this, EventObserver {
            when (it) {
                is SingleRoomData -> {
                    val gson = Gson()
                    val roomData = gson.toJson(it.roomData.roomWithUsers)
                    replaceChatScreenActivity(this, roomData)
                }
                SingleRoomFetchFailed -> Timber.d("Failed to fetch room data")
                else -> Timber.d("Other error")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.getPushNotificationStream(object : SSEListener {
            override fun newMessageReceived(message: Message) {
                Timber.d("Message received")
                runOnUiThread {
                    bindingSetup.cvNotification.tvTitle.text = message.userName
                    bindingSetup.cvNotification.tvMessage.text = message.body?.text
                    bindingSetup.cvNotification.cvRoot.visibility = View.VISIBLE

                    bindingSetup.cvNotification.cvRoot.setOnClickListener {
                        message.roomId?.let { viewModel.getSingleRoomData(it) }
                    }

//                    Glide.with(this@MainActivity).load()
//                    .into(bindingSetup.cvNotification.ivUserImage)
                    Handler(Looper.getMainLooper()).postDelayed({
                        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
                    }, 3000)
                }
            }
        }).asLiveData(Dispatchers.IO).observe(this) {
            Timber.d("Observing SSE")
        }
    }

    override fun onBackPressed() {
        val fragment =
            this.supportFragmentManager.findFragmentById(R.id.main_chat_container) as? NavHostFragment
        val currentFragment =
            fragment?.childFragmentManager?.fragments?.get(0) as? ChatOnBackPressed

        // Check why this returns null if upload is not in progress
        currentFragment?.onBackPressed()?.takeIf { !it }.let {
            Timber.d("Boolean: $it")
            if (it == null) {
                super.onBackPressed()
            }
        }
    }


}

interface ChatOnBackPressed {
    fun onBackPressed(): Boolean
}