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

        viewModel.searchMessageId.value = intent.getIntExtra(Const.Navigation.SEARCH_MESSAGE_ID, 0)
        viewModel.roomWithUsers.value =
            intent.getParcelableExtra(Const.Navigation.ROOM_DATA)

        Timber.d("Load check: ChatScreenActivity created")

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
