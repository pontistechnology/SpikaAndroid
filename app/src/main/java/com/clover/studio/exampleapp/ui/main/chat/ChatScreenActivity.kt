package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.get
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
private const val THUMBNAIL_WIDTH = 256

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var roomWithUsers: RoomWithUsers
    private lateinit var bindingSetup: ActivityChatScreenBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messages: MutableList<Message>
    private var unsentMessages: MutableList<Message> = ArrayList()
    private var currentPhotoLocation: MutableList<Uri> = ArrayList()
    private var filesSelected: MutableList<Uri> = ArrayList()
    private var thumbnailUris: MutableList<Uri> = ArrayList()
    private var photoImageUri: Uri? = null
    private var isAdmin = false
    private var uploadIndex = 0
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<ConstraintLayout>

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val chooseFileContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it != null) {
                for (uri in it) {
                    displayFileInContainer()
                    runOnUiThread { showSendButton() }
                    filesSelected.add(uri)
                }
            }
        }

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            if (it != null) {
                for (uri in it) {
                    convertImageToBitmap(uri)
                }
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                if (photoImageUri != null) {
                    convertImageToBitmap(photoImageUri)
                } else {
                    Timber.d("Photo error")
                }
            } else Timber.d("Photo error")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        bottomSheetBehaviour = BottomSheetBehavior.from(bindingSetup.bottomSheet.root)

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

        bindingSetup.bottomSheet.btnFiles.setOnClickListener {
            chooseFile()
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
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
                showSendButton()
            } else {
                hideSendButton()
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
            if (currentPhotoLocation.isNotEmpty()) {
                uploadImage()
            } else if (filesSelected.isNotEmpty()) {
                uploadFile(filesSelected[0])
            } else {
                createTempMessage()
                sendMessage()
            }
        }

        bindingSetup.ivAdd.setOnClickListener {
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            bindingSetup.vTransparent.visibility = View.VISIBLE
        }

        bindingSetup.bottomSheet.ivRemove.setOnClickListener {
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            bindingSetup.vTransparent.visibility = View.GONE
        }
    }

    private fun hideSendButton() {
        bindingSetup.ivCamera.visibility = View.VISIBLE
        bindingSetup.ivMicrophone.visibility = View.VISIBLE
        bindingSetup.ivButtonSend.visibility = View.GONE
        bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = bindingSetup.ivCamera.id
        }
        bindingSetup.ivAdd.rotation = ROTATION_OFF
    }

    private fun showSendButton() {
        bindingSetup.ivCamera.visibility = View.GONE
        bindingSetup.ivMicrophone.visibility = View.GONE
        bindingSetup.ivButtonSend.visibility = View.VISIBLE
        bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = bindingSetup.ivButtonSend.id
        }
        bindingSetup.ivAdd.rotation = ROTATION_ON
    }

    private fun sendMessage() {
        sendMessage(isImage = false, isFile = false, 0, 0)
    }

    private fun sendMessage(isImage: Boolean, isFile: Boolean, fileId: Long, thumbId: Long) {
        val jsonObject = JsonObject()
        val innerObject = JsonObject()
        innerObject.addProperty(
            Const.JsonFields.TEXT,
            bindingSetup.etMessage.text.toString()
        )
        if (isImage) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.CHAT_IMAGE)
        } else if (isFile) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.FILE_TYPE)
        } else jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.TEXT)

        jsonObject.addProperty(Const.JsonFields.ROOM_ID, roomWithUsers.room.roomId)
        jsonObject.add(Const.JsonFields.BODY, innerObject)

        viewModel.sendMessage(jsonObject)
    }

    private fun createTempMessage() {
        val tempMessage = Message(
            0,
            viewModel.getLocalUserId(),
            0,
            0,
            0,
            -1,
            0,
            roomWithUsers.room.roomId,
            Const.JsonFields.TEXT,
            MessageBody(bindingSetup.etMessage.text.toString(), 1, 1, null, null),
            System.currentTimeMillis()
        )

        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    override fun onBackPressed() {
        // Update room visited
        roomWithUsers.room.visitedRoom = System.currentTimeMillis()
        viewModel.updateRoomVisitedTimestamp(roomWithUsers.room)
        finish()
    }

    private fun chooseFile() {
        chooseFileContract.launch(arrayOf(Const.JsonFields.FILE))
    }

    private fun chooseImage() {
        chooseImageContract.launch(Const.JsonFields.IMAGE)
    }

    private fun takePhoto() {
        photoImageUri = FileProvider.getUriForFile(
            this,
            "com.clover.studio.exampleapp.fileprovider",
            Tools.createImageFile(
                (this)
            )
        )
        takePhotoContract.launch(photoImageUri)
    }

    private fun uploadThumbnail(messageBody: MessageBody, index: Int) {
        uploadImage(messageBody, true, thumbnailUris[index])
    }

    private fun uploadImage() {
        val messageBody = MessageBody("", 0, 0, null, null)
        uploadThumbnail(messageBody, uploadIndex)
//        uploadFile(messageBody, false, currentPhotoLocation[uploadIndex])
    }

    private fun uploadFile(uri: Uri) {
        val messageBody = MessageBody("", 0, 0, null, null)
        val inputStream =
            this.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(this, inputStream!!, contentResolver.getType(uri)!!)
        val uploadPieces =
            if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                fileStream.length() / CHUNK_SIZE + 1
            else fileStream.length() / CHUNK_SIZE
        var progress = 0

        val imageContainer = bindingSetup.llImagesContainer[uploadIndex] as ImageSelectedContainer
        imageContainer.setMaxProgress(uploadPieces.toInt())
        Timber.d("File upload start")
        CoroutineScope(Dispatchers.IO).launch {
            uploadDownloadManager.uploadFile(
                this@ChatScreenActivity,
                uri,
                Const.JsonFields.FILE,
                Const.JsonFields.FILE_TYPE,
                uploadPieces,
                fileStream,
                false,
                object : FileUploadListener {
                    override fun filePieceUploaded() {
                        if (progress <= uploadPieces) {
                            imageContainer.setUploadProgress(progress)
                            progress++
                        } else progress = 0
                    }

                    override fun fileUploadError(description: String) {
                        this@ChatScreenActivity.runOnUiThread {
                            showUploadError(description)
                            imageContainer.hideProgressScreen()
                        }
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        this@ChatScreenActivity.runOnUiThread {
                            Timber.d("Successfully sent file")
                            imageContainer.removeView(imageContainer[0])
                            if (fileId > 0) messageBody.fileId = fileId
                            sendMessage(
                                isImage = false,
                                isFile = true,
                                messageBody.fileId!!,
                                0
                            )

                            uploadIndex++
                            if (uploadIndex < filesSelected.size) {
                                uploadFile(filesSelected[uploadIndex])
                            } else {
                                uploadIndex = 0
                                filesSelected.clear()
                            }
                        }

                        // update room data
                    }
                })
        }
    }

    private fun uploadImage(messageBody: MessageBody, isThumbnail: Boolean, uri: Uri) {
        val inputStream =
            this.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(this, inputStream!!, contentResolver.getType(uri)!!)
        val uploadPieces =
            if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                fileStream.length() / CHUNK_SIZE + 1
            else fileStream.length() / CHUNK_SIZE
        var progress = 0

        val imageContainer = bindingSetup.llImagesContainer[uploadIndex] as ImageSelectedContainer
        imageContainer.setMaxProgress(uploadPieces.toInt())
        Timber.d("File upload start")
        CoroutineScope(Dispatchers.IO).launch {
            uploadDownloadManager.uploadFile(
                this@ChatScreenActivity,
                uri,
                Const.JsonFields.IMAGE,
                Const.JsonFields.AVATAR,
                uploadPieces,
                fileStream,
                isThumbnail,
                object : FileUploadListener {
                    override fun filePieceUploaded() {
                        if (progress <= uploadPieces) {
                            imageContainer.setUploadProgress(progress)
                            progress++
                        } else progress = 0
                    }

                    override fun fileUploadError(description: String) {
                        this@ChatScreenActivity.runOnUiThread {
                            showUploadError(description)
                            imageContainer.hideProgressScreen()
                        }
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        this@ChatScreenActivity.runOnUiThread {
                            if (!isThumbnail) {
                                if (fileId > 0) messageBody.fileId = fileId
                                sendMessage(
                                    isImage = true,
                                    isFile = false,
                                    messageBody.fileId!!,
                                    messageBody.thumbId!!
                                )

                                // TODO think about changing this... Index changes for other views when removed
                                imageContainer.removeView(imageContainer[0])
                                uploadIndex++
                                if (uploadIndex < currentPhotoLocation.size) {
                                    uploadImage()
                                } else {
                                    uploadIndex = 0
                                    currentPhotoLocation.clear()
                                }
                            } else {
                                if (thumbId > 0) messageBody.thumbId = thumbId
                                uploadImage(messageBody, false, currentPhotoLocation[uploadIndex])
                                imageContainer.hideProgressScreen()
                            }
                        }

                        // update room data
                    }
                })
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

    private fun displayFileInContainer() {
        val imageSelected = ImageSelectedContainer(this, null)
        bindingSetup.llImagesContainer.addView(imageSelected)
    }

    private fun convertImageToBitmap(imageUri: Uri?) {
        val bitmap =
            Tools.handleSamplingAndRotationBitmap(this, imageUri)
        val bitmapUri = Tools.convertBitmapToUri(this, bitmap!!)

        val imageSelected = ImageSelectedContainer(this, null)
        bitmap.let { imageBitmap -> imageSelected.setImage(imageBitmap) }
        bindingSetup.llImagesContainer.addView(imageSelected)

        runOnUiThread { showSendButton() }
        imageSelected.setButtonListener(object :
            ImageSelectedContainer.RemoveImageSelected {
            override fun removeImage() {
                bindingSetup.llImagesContainer.removeView(imageSelected)
            }
        })
        val thumbnail =
            ThumbnailUtils.extractThumbnail(bitmap, bitmap.width, bitmap.height)
        val thumbnailUri = Tools.convertBitmapToUri(this, thumbnail)
        // Create thumbnail for the image which will also be sent to the backend
        thumbnailUris.add(thumbnailUri)
        currentPhotoLocation.add(bitmapUri)
    }
}