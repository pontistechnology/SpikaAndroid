package com.clover.studio.spikamessenger.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.ActivityChatScreenBinding
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseActivity
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


fun startChatScreenActivity(
    fromActivity: Activity,
    roomWithUsers: RoomWithUsers,
    searchMessageId: Int? = 0
) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        if (searchMessageId != 0) {
            intent.putExtra(Const.Navigation.SEARCH_MESSAGE_ID, searchMessageId)
        }
        intent.putExtra(Const.Navigation.ROOM_DATA, roomWithUsers)
        startActivity(intent)
    }

fun replaceChatScreenActivity(
    fromActivity: Activity,
    roomWithUsers: RoomWithUsers,
    searchMessageId: Int? = 0
) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        if (searchMessageId != 0) {
            intent.putExtra(Const.Navigation.SEARCH_MESSAGE_ID, searchMessageId)
        }
        intent.putExtra(Const.Navigation.ROOM_DATA, roomWithUsers)
        startActivity(intent)
        finish()
    }

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    var roomWithUsers: RoomWithUsers? = null

    private lateinit var bindingSetup: ActivityChatScreenBinding
    private val viewModel: ChatViewModel by viewModels()

    /** These two fields are used for the room notification, which has been removed temporarily **/
//    private var handler = Handler(Looper.getMainLooper())
//    private var runnable: Runnable = Runnable {
//        Timber.d("Ending handler")
//        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
//    }

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)

        val view = bindingSetup.root
        setContentView(view)

        viewModel.searchMessageId.value = intent.getIntExtra(Const.Navigation.SEARCH_MESSAGE_ID, 0)
        viewModel.roomWithUsers.value =
            intent.getParcelableExtra(Const.Navigation.ROOM_DATA, RoomWithUsers::class.java)

        if (viewModel.roomWithUsers.value == null) {
            val extras = intent.getIntExtra(Const.IntentExtras.ROOM_ID_EXTRA, 0)
            viewModel.roomId.value = extras
        }

        Timber.d("Load check: ChatScreenActivity created")

        initializeObservers()
    }

    private fun initializeObservers() {
        /** Room notification has been disabled until we decide how to implement it correctly **/
//        viewModel.roomNotificationListener.observe(this, EventObserver {
//            when (it.response.status) {
//                Resource.Status.SUCCESS -> {
//                    val myUserId = viewModel.getLocalUserId()
//
//                    if (myUserId == it.message.fromUserId || roomWithUsers?.room?.roomId == it.message.roomId || it.response.responseData?.room!!.muted) return@EventObserver
//                    runOnUiThread {
//                        val animator =
//                            ValueAnimator.ofInt(
//                                bindingSetup.cvNotification.pbTimeout.max,
//                                0
//                            )
//                        animator.duration = 5000
//                        animator.addUpdateListener { animation ->
//                            bindingSetup.cvNotification.pbTimeout.progress =
//                                animation.animatedValue as Int
//                        }
//                        animator.start()
//
//                        if (it.response.responseData.room.type.equals(Const.JsonFields.GROUP)) {
//                            Glide.with(this@ChatScreenActivity)
//                                .load(it.response.responseData.room.avatarFileId?.let { fileId ->
//                                    Tools.getFilePathUrl(
//                                        fileId
//                                    )
//                                })
//                                .placeholder(R.drawable.img_user_placeholder)
//                                .centerCrop()
//                                .into(bindingSetup.cvNotification.ivUserImage)
//                            bindingSetup.cvNotification.tvTitle.text =
//                                it.response.responseData.room.name
//                            for (user in it.response.responseData.users) {
//                                if (user.id != myUserId && user.id == it.message.fromUserId) {
//                                    val content: String =
//                                        if (it.message.type != Const.JsonFields.TEXT_TYPE) {
//                                            user.formattedDisplayName + ": " + getString(
//                                                R.string.generic_shared,
//                                                it.message.type.toString()
//                                                    .replaceFirstChar { type -> type.uppercase() })
//                                        } else {
//                                            user.formattedDisplayName + ": " + it.message.body?.text.toString()
//                                        }
//
//                                    bindingSetup.cvNotification.tvMessage.text =
//                                        content
//                                    break
//                                }
//                            }
//                        } else {
//                            for (user in it.response.responseData.users) {
//                                if (user.id != myUserId && user.id == it.message.fromUserId) {
//                                    Glide.with(this@ChatScreenActivity)
//                                        .load(user.avatarFileId?.let { fileId ->
//                                            Tools.getFilePathUrl(
//                                                fileId
//                                            )
//                                        })
//                                        .placeholder(R.drawable.img_user_placeholder)
//                                        .centerCrop()
//                                        .into(bindingSetup.cvNotification.ivUserImage)
//                                    val content: String =
//                                        if (it.message.type != Const.JsonFields.TEXT_TYPE) {
//                                            getString(
//                                                R.string.generic_shared,
//                                                it.message.type.toString()
//                                                    .replaceFirstChar { type -> type.uppercase() })
//                                        } else {
//                                            it.message.body?.text.toString()
//                                        }
//
//                                    bindingSetup.cvNotification.tvTitle.text =
//                                        user.formattedDisplayName
//                                    bindingSetup.cvNotification.tvMessage.text =
//                                        content
//                                    break
//                                }
//                            }
//                        }
//
//
//                        bindingSetup.cvNotification.cvRoot.visibility = View.VISIBLE
//
//                        val roomId = it.message.roomId
//                        bindingSetup.cvNotification.cvRoot.setOnClickListener {
//                            roomId?.let { roomId -> viewModel.getSingleRoomData(roomId) }
//                        }
//
//                        runnable.let { runnable -> handler.removeCallbacks(runnable) }
//
//                        handler = Handler(Looper.getMainLooper())
//                        Timber.d("Starting handler")
//                        handler.postDelayed(runnable, 5000)
//                    }
//                }
//
//                Resource.Status.ERROR -> Timber.d("Failed to fetch room with users")
//                else -> Timber.d("Other error")
//            }
//        })

        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
                DialogError.getInstance(this,
                    getString(R.string.warning),
                    getString(R.string.session_expired),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            // Ignore
                        }

                        override fun onSecondOptionClicked() {
                            viewModel.setTokenExpiredFalse()
                            viewModel.removeToken()
                            startOnboardingActivity(this@ChatScreenActivity, false)
                        }
                    })
            }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.getUnreadCount()
    }
}

interface ChatOnBackPressed {
    fun onBackPressed(): Boolean
}
