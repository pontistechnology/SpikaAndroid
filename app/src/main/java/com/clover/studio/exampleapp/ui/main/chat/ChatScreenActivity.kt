package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageBody
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.ui.ImageSelectedContainer
import com.clover.studio.exampleapp.ui.main.chat_details.startChatDetailsActivity
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }

private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var roomWithUsers: RoomWithUsers
    private lateinit var bindingSetup: ActivityChatScreenBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messages: MutableList<Message>
    private var unsentMessages: MutableList<Message> = ArrayList()
    private var currentPhotoLocation: MutableList<Uri> = ArrayList()
    private var isAdmin = false

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            if (it != null) {
                Timber.d("$it")
                for (uri in it) {
                    val bitmap =
                        Tools.handleSamplingAndRotationBitmap(this, uri)
                    val bitmapUri = Tools.convertBitmapToUri(this, bitmap!!)

                    val imageSelected = ImageSelectedContainer(this, null)
                    bitmap.let { imageBitmap -> imageSelected.setImage(imageBitmap) }
                    bindingSetup.llImagesContainer.addView(imageSelected)
                    imageSelected.setButtonListener(object :
                        ImageSelectedContainer.RemoveImageSelected {
                        override fun removeImage() {
                            bindingSetup.llImagesContainer.removeView(imageSelected)
                        }
                    })
                    currentPhotoLocation.add(bitmapUri)
                }
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(this, currentPhotoLocation[0])
                val bitmapUri = Tools.convertBitmapToUri(this, bitmap!!)

                val imageSelected = ImageSelectedContainer(this, null)
                bitmap.let { imageBitmap -> imageSelected.setImage(imageBitmap) }
                bindingSetup.llImagesContainer.addView(imageSelected)
                imageSelected.setButtonListener(object :
                    ImageSelectedContainer.RemoveImageSelected {
                    override fun removeImage() {
                        bindingSetup.llImagesContainer.removeView(imageSelected)
                    }
                })
                currentPhotoLocation.add(bitmapUri)
            } else {
                Timber.d("Photo error")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        val gson = Gson()
        // Fetch room data sent from previous activity
        roomWithUsers = gson.fromJson(
            intent.getStringExtra(Const.Navigation.ROOM_DATA),
            RoomWithUsers::class.java
        )

        initViews()
        setUpAdapter()
        initializeObservers()
        checkIsUserAdmin()
    }

    private fun checkIsUserAdmin() {
        for (user in roomWithUsers.users) {
            isAdmin = user.id == viewModel.getLocalUserId() && viewModel.isUserAdmin(
                roomWithUsers.room.roomId,
                user.id
            )

            if (isAdmin) break
        }
    }

    private fun initializeObservers() {
        viewModel.messageSendListener.observe(this, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_SENT -> {
                    bindingSetup.etMessage.setText("")
                    viewModel.deleteLocalMessages(unsentMessages)
                    unsentMessages.clear()
                }
                ChatStatesEnum.MESSAGE_SEND_FAIL -> Timber.d("Message send fail")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesListener.observe(this, EventObserver {
            when (it) {
                is MessagesFetched -> {
                    viewModel.deleteLocalMessages(unsentMessages)
                    unsentMessages.clear()
                }
                is MessageFetchFail -> Timber.d("Failed to fetch messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesTimestampListener.observe(this, EventObserver {
            when (it) {
                is MessagesTimestampFetched -> {
                    Timber.d("Messages timestamp fetched")
                    messages = it.messages as MutableList<Message>
                    chatAdapter.submitList(it.messages)
                }
                is MessageTimestampFetchFail -> Timber.d("Failed to fetch messages timestamp")
                else -> Timber.d("Other error")
            }
        })

        viewModel.sendMessageDeliveredListener.observe(this, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_DELIVERED -> Timber.d("Messages delivered")
                ChatStatesEnum.MESSAGE_DELIVER_FAIL -> Timber.d("Failed to deliver messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getLocalMessages(roomWithUsers.room.roomId).observe(this) {
            if (it.isNotEmpty()) {
                messages = it as MutableList<Message>
                messages.sortByDescending { message -> message.createdAt }
                chatAdapter.submitList(messages) {
                    bindingSetup.rvChat.scrollToPosition(0)
                }
            }
        }
    }

    private fun setUpAdapter() {
        chatAdapter = ChatAdapter(this, viewModel.getLocalUserId()!!, roomWithUsers.users)

        bindingSetup.rvChat.adapter = chatAdapter
        bindingSetup.rvChat.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, true)

        // Add callback for item swipe handling
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT
            ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // ignore
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                // Get swiped message text and add to message EditText
                // After that, return item to correct position
                val position = viewHolder.adapterPosition
                bindingSetup.etMessage.setText(messages[position].body?.text)
                chatAdapter.notifyItemChanged(position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(bindingSetup.rvChat)

        // Notify backend of messages seen
        viewModel.sendMessagesSeen(roomWithUsers.room.roomId)

        // Update room visited
        roomWithUsers.room.visitedRoom = System.currentTimeMillis()
        viewModel.updateRoomVisitedTimestamp(roomWithUsers.room)
    }

    private fun initViews() {
        bindingSetup.tvTitle.setOnClickListener {
            startChatDetailsActivity(this, roomWithUsers.room.roomId, isAdmin)
        }

        bindingSetup.ivArrowBack.setOnClickListener {
            finish()
        }

        bindingSetup.ivCamera.setOnClickListener {
            ChooserDialog.getInstance(this,
                getString(R.string.placeholder_title),
                null,
                getString(R.string.choose_from_gallery),
                getString(R.string.take_photo),
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        chooseImage()
                    }

                    override fun onSecondOptionClicked() {
                        takePhoto()
                    }
                })
        }

        bindingSetup.etMessage.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                bindingSetup.ivCamera.visibility = View.GONE
                bindingSetup.ivMicrophone.visibility = View.GONE
                bindingSetup.ivButtonSend.visibility = View.VISIBLE
                bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = bindingSetup.ivButtonSend.id
                }
                bindingSetup.ivAdd.rotation = ROTATION_ON
            } else {
                bindingSetup.ivCamera.visibility = View.VISIBLE
                bindingSetup.ivMicrophone.visibility = View.VISIBLE
                bindingSetup.ivButtonSend.visibility = View.GONE
                bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = bindingSetup.ivCamera.id
                }
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        }

        // TODO add send message button and handle UI when message is being entered
        // Change required field after work has been done

        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            roomWithUsers.users.forEach { roomUser ->
                if (viewModel.getLocalUserId().toString() != roomUser.id.toString()) {
                    bindingSetup.tvChatName.text = roomUser.displayName
                    Glide.with(this)
                        .load(roomUser.avatarUrl?.let { getAvatarUrl(it) })
                        .into(bindingSetup.ivUserImage)
                }
            }
        } else {
            bindingSetup.tvChatName.text = roomWithUsers.room.name
            Glide.with(this).load(roomWithUsers.room.avatarUrl?.let { getAvatarUrl(it) })
                .into(bindingSetup.ivUserImage)
        }

        bindingSetup.tvTitle.text = roomWithUsers.room.type

        bindingSetup.ivButtonSend.setOnClickListener {
            // TODO success and fail states.
            val tempMessage = Message(
                0,
                viewModel.getLocalUserId(),
                0,
                0,
                0,
                -1,
                0,
                roomWithUsers.room.roomId,
                "text",
                MessageBody(bindingSetup.etMessage.text.toString(), "text"),
                System.currentTimeMillis()
            )

            unsentMessages.add(tempMessage)
            viewModel.storeMessageLocally(tempMessage)

            val jsonObject = JsonObject()
            val innerObject = JsonObject()
            innerObject.addProperty(
                Const.JsonFields.TEXT,
                bindingSetup.etMessage.text.toString()
            )
            innerObject.addProperty(Const.JsonFields.TYPE, "text")

            jsonObject.addProperty(Const.JsonFields.ROOM_ID, roomWithUsers.room.roomId)
            jsonObject.addProperty(Const.JsonFields.TYPE, "text")
            jsonObject.add(Const.JsonFields.BODY, innerObject)

            viewModel.sendMessage(jsonObject)
        }
    }

    override fun onBackPressed() {
        // Update room visited
        roomWithUsers.room.visitedRoom = System.currentTimeMillis()
        viewModel.updateRoomVisitedTimestamp(roomWithUsers.room)
        finish()
    }

    private fun chooseImage() {
        chooseImageContract.launch(Const.JsonFields.IMAGE)
    }

    private fun takePhoto() {
        currentPhotoLocation[0] = FileProvider.getUriForFile(
            this,
            "com.clover.studio.exampleapp.fileprovider",
            Tools.createImageFile(
                (this)
            )
        )
        Timber.d("$currentPhotoLocation")
        takePhotoContract.launch(currentPhotoLocation[0])
    }

    private fun uploadImage() {
        if (currentPhotoLocation != Uri.EMPTY) {
            for (uri in currentPhotoLocation) {
                val inputStream =
                    this.contentResolver.openInputStream(uri)

                val fileStream = Tools.copyStreamToFile(this, inputStream!!)
                val uploadPieces =
                    if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                        fileStream.length() / CHUNK_SIZE + 1
                    else fileStream.length() / CHUNK_SIZE

//            binding.progressBar.max = uploadPieces.toInt()
                Timber.d("File upload start")
                CoroutineScope(Dispatchers.IO).launch {
                    uploadDownloadManager.uploadFile(
                        this@ChatScreenActivity,
                        uri,
                        Const.JsonFields.IMAGE,
                        Const.JsonFields.AVATAR,
                        uploadPieces,
                        fileStream,
                        object : FileUploadListener {
                            override fun filePieceUploaded() {
                                // Update progress
                            }

                            override fun fileUploadError(description: String) {
                                Timber.d("Upload Error")
                                this@ChatScreenActivity.runOnUiThread {
                                    showUploadError(description)
                                }
                            }

                            override fun fileUploadVerified(path: String) {
                                this@ChatScreenActivity.runOnUiThread {
                                    // remove progress and placeholder
                                }

                                // update room data
                            }

                        })
                }
            }
        }
    }

    private fun showUploadError(description: String) {
        DialogError.getInstance(this,
            getString(R.string.error),
            getString(R.string.image_failed_upload, description),
            null,
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    // ignore
                }

                override fun onSecondOptionClicked() {
                    // ignore
                }
            })
    }
}