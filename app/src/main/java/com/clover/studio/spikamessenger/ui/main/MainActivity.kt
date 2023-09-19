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
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.AppPermissions
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

    private fun initializeObservers() {
        viewModel.newMessageReceivedListener.observe(this, EventObserver { message ->
            message.responseData?.roomId?.let {
                viewModel.getRoomWithUsers(
                    it,
                    message.responseData
                )
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
            if (AppPermissions.hasPostNotificationPermission()) {
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
