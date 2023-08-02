package com.clover.studio.spikamessenger.ui.main

import android.Manifest
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.ActivityMainBinding
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.AppPermissions.notificationPermission
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseActivity
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.PhonebookService
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingSetup: ActivityMainBinding
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var phonebookService: Intent? = null

    /** These two fields are used for the room notification, which has been removed temporarily **/
//    private var handler = Handler(Looper.getMainLooper())
//    private var runnable: Runnable = Runnable {
//        Timber.d("Ending handler")
//        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.getUserTheme() == MODE_NIGHT_UNSPECIFIED) {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            when (uiModeManager.nightMode) {
                UiModeManager.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )

                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } else {
            AppCompatDelegate.setDefaultNightMode(viewModel.getUserTheme()!!)
        }

        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        checkIntentExtras()
        checkNotificationPermission()
        initializeObservers()
        sendPushTokenToServer()
        startPhonebookService()
    }

    private fun startPhonebookService() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("Starting phonebook service")
            phonebookService = Intent(this, PhonebookService::class.java)
            startService(phonebookService)
        }
    }

    private fun checkIntentExtras() {
        val extras = intent?.extras
        if (extras != null) {
            Timber.d("Extras: ${extras.getInt(Const.IntentExtras.ROOM_ID_EXTRA)}")
            try {
                viewModel.getSingleRoomData(extras.getInt(Const.IntentExtras.ROOM_ID_EXTRA))
            } catch (ex: Exception) {
                // Ignore
            }
            intent.removeExtra(Const.IntentExtras.ROOM_ID_EXTRA)
        }
    }

    private fun initializeObservers() {
        viewModel.newMessageReceivedListener.observe(this, EventObserver { message ->
            message.responseData?.roomId?.let {
                viewModel.getRoomWithUsers(
                    it,
                    message.responseData
                )
            }
        })

        viewModel.roomDataListener.observe(this, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    it.responseData?.roomWithUsers?.let { roomWithUsers ->
                        startChatScreenActivity(
                            this,
                            roomWithUsers.room.roomId
                        )
                    }
                    Timber.d("Main Success!")
                }

                Resource.Status.ERROR -> Timber.d("Failed to fetch room data")
                else -> Timber.d("Other error")
            }
        })

        viewModel.deleteUserListener.observe(this, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        Tools.clearUserData(this@MainActivity)
                    }
                }

                Resource.Status.ERROR -> Timber.d("Delete user failed")
                else -> Timber.d("Other error")
            }
        })

        /** Room notification has been disabled until we decide how to implement it correctly **/
//        viewModel.roomNotificationListener.observe(this, EventObserver {
//            when (it.response.status) {
//                Resource.Status.SUCCESS -> {
//                    val myUserId = viewModel.getLocalUserId()
//
//                    if (myUserId == it.message.fromUserId || it.response.responseData?.room?.muted == true) return@EventObserver
//                    runOnUiThread {
//                        val animator =
//                            ValueAnimator.ofInt(bindingSetup.cvNotification.pbTimeout.max, 0)
//                        animator.duration = 5000
//                        animator.addUpdateListener { animation ->
//                            bindingSetup.cvNotification.pbTimeout.progress =
//                                animation.animatedValue as Int
//                        }
//                        animator.start()
//
//                        if (it.response.responseData!!.room.type.equals(Const.JsonFields.GROUP)) {
//                            Timber.d("Showing room image")
//                            Glide.with(this@MainActivity)
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
//                                    val content =
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
//                                    Glide.with(this@MainActivity)
//                                        .load(user.avatarFileId?.let { fileId ->
//                                            Tools.getFilePathUrl(
//                                                fileId
//                                            )
//                                        })
//                                        .centerCrop()
//                                        .placeholder(R.drawable.img_user_placeholder)
//                                        .into(bindingSetup.cvNotification.ivUserImage)
//                                    val content =
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
//                            roomId?.let { roomId ->
//                                run {
//                                    viewModel.getSingleRoomData(roomId)
//                                    bindingSetup.cvNotification.cvRoot.visibility = View.GONE
//                                }
//                            }
//                        }
//
//                        // Remove old instance of runnable if any is active. Prevents older
//                        // notifications from removing newer ones.
//                        Timber.d("Starting handler 1")
//                        handler.removeCallbacks(runnable)
//
//                        handler = Handler(Looper.getMainLooper())
//                        Timber.d("Starting handler 2")
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
                            startOnboardingActivity(this@MainActivity, false)
                        }
                    })
            }
        })

        viewModel.isInTeamMode()
    }

    private fun checkNotificationPermission() {
        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (!it) {
                    Timber.d("Couldn't send notifications. No permission granted.")
                }
            }

        if (
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // ignore
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) || shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            // TODO show why permission is needed
        } else {
            if (notificationPermission.isNotEmpty()) {
                notificationPermissionLauncher.launch(notificationPermission)
            }
        }
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIntentExtras()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("First SSE launch = ${viewModel.checkIfFirstSSELaunch()}")
        if (viewModel.checkIfFirstSSELaunch()) {
            viewModel.getPushNotificationStream().asLiveData().observe(this) {}
        }

        viewModel.getUnreadCount()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (phonebookService != null) {
            stopService(phonebookService)
        }
    }
}