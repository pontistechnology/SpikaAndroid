package com.clover.studio.exampleapp.ui.main.chat

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageBody
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.FragmentChatMessagesBinding
import com.clover.studio.exampleapp.ui.ImageSelectedContainer
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/*fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }
*/
private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f
//private const val THUMBNAIL_WIDTH = 256

@AndroidEntryPoint
class ChatMessagesFragment : BaseFragment() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var roomWithUsers: RoomWithUsers
    private lateinit var bindingSetup: FragmentChatMessagesBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messages: MutableList<Message>
    private var unsentMessages: MutableList<Message> = ArrayList()

    private var currentPhotoLocation: MutableList<Uri> = ArrayList()
    private var currentVideoLocation: MutableList<Uri> = ArrayList()

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
            bindingSetup.llImagesContainer.removeAllViews()
            if (it != null) {
                for (uri in it) {
                    displayFileInContainer(uri)
                    activity!!.runOnUiThread { showSendButton() }
                    filesSelected.add(uri)
                }
            }
        }

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            bindingSetup.llImagesContainer.removeAllViews()
            if (it != null) {
                for (uri in it) {
                    getImageOrVideo(uri)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        bindingSetup = FragmentChatMessagesBinding.inflate(layoutInflater)

        bottomSheetBehaviour = BottomSheetBehavior.from(bindingSetup.bottomSheet.root)

        roomWithUsers = (activity as ChatScreenActivity?)!!.roomWithUsers!!
        Timber.d("chat activity: $roomWithUsers")

        setInformation(roomWithUsers)

        initViews()
        setUpAdapter()
        initializeObservers()
        checkIsUserAdmin()

        return bindingSetup.root
    }

    private fun setInformation(roomWithUsers: RoomWithUsers) {
        Timber.d("entered")
        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            setName(roomWithUsers)
            val avatarUrl = setIcon(roomWithUsers)
            Glide.with(this)
                .load(avatarUrl.let { Tools.getFileUrl(it) })
                .into(bindingSetup.ivUserImage)

        } else {
            bindingSetup.tvChatName.text = roomWithUsers.room.name
            Glide.with(this).load(roomWithUsers.room.avatarUrl?.let { Tools.getFileUrl(it) })
                .into(bindingSetup.ivUserImage)
        }
    }

    private fun setIcon(roomWithUsers: RoomWithUsers): String {
        var avatarUrl = ""
        if (roomWithUsers.room.avatarUrl != "") {
            avatarUrl = roomWithUsers.room.avatarUrl.toString()
        } else {
            for (user in roomWithUsers.users) {
                if (user.id != viewModel.getLocalUserId()) {
                    avatarUrl = user.avatarUrl.toString()
                }
            }
        }
        return avatarUrl
    }

    private fun setName(roomWithUsers: RoomWithUsers) {
        if (roomWithUsers.room.name != "") {
            bindingSetup.tvChatName.text = roomWithUsers.room.name
        } else {
            bindingSetup.tvChatName.text = roomWithUsers.users[0].displayName
        }
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
        viewModel.messageSendListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_SENT -> {
                    bindingSetup.etMessage.setText("")
                    viewModel.deleteLocalMessages(unsentMessages)
                    unsentMessages.clear()
                    chatAdapter.notifyDataSetChanged()

                }
                ChatStatesEnum.MESSAGE_SEND_FAIL -> Timber.d("Message send fail")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is MessagesFetched -> {
                    viewModel.deleteLocalMessages(unsentMessages)
                    unsentMessages.clear()
                }
                is MessageFetchFail -> Timber.d("Failed to fetch messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesTimestampListener.observe(viewLifecycleOwner, EventObserver {
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

        viewModel.sendMessageDeliveredListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_DELIVERED -> {
                    Timber.d("Messages delivered")
                    unsentMessages.clear()

                }
                ChatStatesEnum.MESSAGE_DELIVER_FAIL -> Timber.d("Failed to deliver messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getLocalMessages(roomWithUsers.room.roomId).observe(viewLifecycleOwner) {
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
        chatAdapter = ChatAdapter(context!!, viewModel.getLocalUserId()!!, roomWithUsers.users)

        bindingSetup.rvChat.adapter = chatAdapter
        bindingSetup.rvChat.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, true)

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
            val action =
                ChatMessagesFragmentDirections.actionChatMessagesFragmentToChatDetailsFragment(
                    roomWithUsers.room.roomId,
                    isAdmin
                )
            findNavController().navigate(action)
        }

        bindingSetup.ivArrowBack.setOnClickListener {
            onBackPressed()
        }

        bindingSetup.bottomSheet.btnFiles.setOnClickListener {
            chooseFile()
            rotationAnimation()
        }

        bindingSetup.ivCamera.setOnClickListener {
            ChooserDialog.getInstance(context!!,
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

        bindingSetup.tvTitle.text = roomWithUsers.room.type

        bindingSetup.ivButtonSend.setOnClickListener {
            if (currentPhotoLocation.isNotEmpty()) {
                uploadImage()

            } else if (filesSelected.isNotEmpty()) {
                uploadFile(filesSelected[0])

            } else if (currentVideoLocation.isNotEmpty()) {
                uploadVideo()

            } else {
                createTempMessage()
                sendMessage()
            }

            hideSendButton()
        }

        bindingSetup.ivAdd.setOnClickListener {
            if (bottomSheetBehaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
                bindingSetup.ivAdd.rotation = ROTATION_ON
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                bindingSetup.vTransparent.visibility = View.VISIBLE
            }
        }

        bindingSetup.bottomSheet.ivRemove.setOnClickListener {
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            rotationAnimation()
        }

        bindingSetup.bottomSheet.btnLibrary.setOnClickListener {
            chooseImage()
            rotationAnimation()
        }

        bindingSetup.bottomSheet.btnLocation.setOnClickListener {
            rotationAnimation()
        }
        bindingSetup.bottomSheet.btnContact.setOnClickListener {
            rotationAnimation()
        }
    }

    private fun rotationAnimation() {
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        bindingSetup.vTransparent.visibility = View.GONE
        bindingSetup.ivAdd.rotation = ROTATION_OFF
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
        sendMessage(isImage = false, isFile = false, isVideo = false, 0, 0)
    }

    private fun sendMessage(
        isImage: Boolean,
        isFile: Boolean,
        isVideo: Boolean,
        fileId: Long,
        thumbId: Long
    ) {
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
        } else if (isVideo) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.VIDEO)
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

    private fun onBackPressed() {
        // Update room visited
        roomWithUsers.room.visitedRoom = System.currentTimeMillis()
        viewModel.updateRoomVisitedTimestamp(roomWithUsers.room)
        //
        activity!!.finish()
    }

    private fun chooseFile() {
        chooseFileContract.launch(arrayOf(Const.JsonFields.FILE))
    }

    private fun chooseImage() {
        chooseImageContract.launch(arrayOf(Const.JsonFields.FILE))
    }

    private fun takePhoto() {
        photoImageUri = FileProvider.getUriForFile(
            context!!,
            "com.clover.studio.exampleapp.fileprovider",
            Tools.createImageFile(
                (activity!!)
            )
        )
        takePhotoContract.launch(photoImageUri)
    }

    private fun uploadThumbnail(messageBody: MessageBody, index: Int) {
        uploadImage(messageBody, true, thumbnailUris[index])
    }

    private fun uploadVideoThumbnail(messageBody: MessageBody, index: Int) {
        uploadVideo(messageBody, true, thumbnailUris[index])
    }

    private fun uploadImage() {
        val messageBody = MessageBody("", 0, 0, null, null)
        uploadThumbnail(messageBody, uploadIndex)
//        uploadFile(messageBody, false, currentPhotoLocation[uploadIndex])
    }

    private fun uploadVideo() {
        val messageBody = MessageBody("", 0, 0, null, null)
        uploadVideoThumbnail(messageBody, uploadIndex)
//        uploadFile(messageBody, false, currentPhotoLocation[uploadIndex])
    }

    private fun uploadFile(uri: Uri) {
        val messageBody = MessageBody("", 0, 0, null, null)
        val inputStream =
            activity!!.contentResolver.openInputStream(uri)

        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        val cr = activity!!.contentResolver
        cr.query(uri, projection, null, null, null)?.use { metaCursor ->
            if (metaCursor.moveToFirst()) {
                fileName = metaCursor.getString(0)
            }
        }

        Tools.fileName = fileName

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(uri)!!
        )
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
                activity!!,
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
                        activity!!.runOnUiThread {
                            if (imageContainer.childCount > 0) {
                                imageContainer.removeViewAt(0)
                            }
                            uploadIndex++
                            if (uploadIndex < filesSelected.size) {
                                uploadFile(filesSelected[uploadIndex])
                            } else {
                                uploadIndex = 0
                                filesSelected.clear()
                            }

                            Toast.makeText(
                                activity!!.baseContext,
                                getString(R.string.failed_file_upload),
                                Toast.LENGTH_SHORT
                            ).show()
//                            showUploadError(description)
//                            imageContainer.hideProgressScreen()
                        }
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        activity!!.runOnUiThread {
                            Timber.d("Successfully sent file")
                            if (imageContainer.childCount > 0) {
                                imageContainer.removeViewAt(0)
                            }

                            if (fileId > 0) messageBody.fileId = fileId
                            sendMessage(
                                isImage = false,
                                isFile = true,
                                isVideo = false,
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

    private fun uploadVideo(messageBody: MessageBody, isThumbnail: Boolean, uri: Uri) {
        val inputStream =
            activity!!.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(uri)!!
        )
        val uploadPieces =
            if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                fileStream.length() / CHUNK_SIZE + 1
            else fileStream.length() / CHUNK_SIZE

        Timber.d("upload pieces: $uploadPieces")
        var progress = 0

        val imageContainer = bindingSetup.llImagesContainer[uploadIndex] as ImageSelectedContainer
        imageContainer.setMaxProgress(uploadPieces.toInt())
        Timber.d("File upload start")
        CoroutineScope(Dispatchers.IO).launch {
            uploadDownloadManager.uploadFile(
                activity!!,
                uri,
                Const.JsonFields.VIDEO,
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
                        activity!!.runOnUiThread {
                            if (imageContainer.childCount > 0) {
                                imageContainer.removeViewAt(0)
                            }
                            uploadIndex++
                            if (uploadIndex < currentPhotoLocation.size) {
                                uploadVideo()
                            } else {
                                uploadIndex = 0
                                currentVideoLocation.clear()
                            }

                            Toast.makeText(
                                activity!!.baseContext,
                                getString(R.string.failed_file_upload),
                                Toast.LENGTH_SHORT
                            ).show()
//                            showUploadError(description)
//                            imageContainer.hideProgressScreen()
                        }
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        activity!!.runOnUiThread {
                            if (!isThumbnail) {
                                if (fileId > 0) messageBody.fileId = fileId
                                sendMessage(
                                    isImage = false,
                                    isFile = false,
                                    isVideo = true,
                                    messageBody.fileId!!,
                                    messageBody.thumbId!!
                                )

                                // TODO think about changing this... Index changes for other views when removed
                                if (imageContainer.childCount > 0) {
                                    imageContainer.removeViewAt(0)
                                }
                                uploadIndex++
                                if (uploadIndex < currentPhotoLocation.size) {
                                    uploadVideo()
                                } else {
                                    uploadIndex = 0
                                    currentVideoLocation.clear()
                                }
                            } else {
                                if (thumbId > 0) messageBody.thumbId = thumbId
                                uploadVideo(messageBody, false, currentVideoLocation[uploadIndex])
                                imageContainer.hideProgressScreen()
                            }
                        }
                        // update room data
                    }
                })
        }
    }

    private fun uploadImage(messageBody: MessageBody, isThumbnail: Boolean, uri: Uri) {
        val inputStream =
            activity!!.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(uri)!!
        )
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
                activity!!,
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
                        activity!!.runOnUiThread {
                            if (imageContainer.childCount > 0) {
                                imageContainer.removeViewAt(0)
                            }
                            uploadIndex++
                            if (uploadIndex < currentPhotoLocation.size) {
                                uploadImage()
                            } else {
                                uploadIndex = 0
                                currentPhotoLocation.clear()
                            }

                            Toast.makeText(
                                activity!!.baseContext,
                                getString(R.string.failed_file_upload),
                                Toast.LENGTH_SHORT
                            ).show()
//                            showUploadError(description)
//                            imageContainer.hideProgressScreen()
                        }
                    }

                    override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                        activity!!.runOnUiThread {
                            if (!isThumbnail) {
                                if (fileId > 0) messageBody.fileId = fileId
                                sendMessage(
                                    isImage = true,
                                    isFile = false,
                                    isVideo = false,
                                    messageBody.fileId!!,
                                    messageBody.thumbId!!
                                )

                                // TODO think about changing this... Index changes for other views when removed
                                if (imageContainer.childCount > 0) {
                                    imageContainer.removeViewAt(0)
                                }
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

    /*private fun showUploadError(description: String) {
        DialogError.getInstance(activity!!,
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
    }*/

    private fun displayFileInContainer(uri: Uri) {
        val imageSelected = ImageSelectedContainer(activity!!, null)
        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        val cr = activity!!.contentResolver
        cr.query(uri, projection, null, null, null)?.use { metaCursor ->
            if (metaCursor.moveToFirst()) {
                fileName = metaCursor.getString(0)
            }
        }

        imageSelected.setFile(cr.getType(uri)!!, fileName)
        imageSelected.setButtonListener(object : ImageSelectedContainer.RemoveImageSelected {
            override fun removeImage() {
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })
        bindingSetup.llImagesContainer.addView(imageSelected)
    }

    private fun getImageOrVideo(uri: Uri) {
        val cR: ContentResolver = context!!.contentResolver
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getExtensionFromMimeType(cR.getType(uri))

        if (type.equals(Const.FileExtensions.MP4)) {
            convertVideo(uri)
        } else {
            convertImageToBitmap(uri)
        }
    }

    private fun convertVideo(videoUri: Uri) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, videoUri)
        val bitMap = mmr.frameAtTime

        val imageSelected = ImageSelectedContainer(activity!!, null)
        bitMap.let { imageBitmap -> imageSelected.setImage(imageBitmap!!) }
        bindingSetup.llImagesContainer.addView(imageSelected)

        activity!!.runOnUiThread { showSendButton() }
        imageSelected.setButtonListener(object :
            ImageSelectedContainer.RemoveImageSelected {
            override fun removeImage() {
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })

        thumbnailUris.add(videoUri)
        currentVideoLocation.add(videoUri)
    }

    private fun convertImageToBitmap(imageUri: Uri?) {
        val bitmap =
            Tools.handleSamplingAndRotationBitmap(activity!!, imageUri)
        val bitmapUri = Tools.convertBitmapToUri(activity!!, bitmap!!)

        val imageSelected = ImageSelectedContainer(context!!, null)
        bitmap.let { imageBitmap -> imageSelected.setImage(imageBitmap) }
        bindingSetup.llImagesContainer.addView(imageSelected)

        activity!!.runOnUiThread { showSendButton() }
        imageSelected.setButtonListener(object :
            ImageSelectedContainer.RemoveImageSelected {
            override fun removeImage() {
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })
        val thumbnail =
            ThumbnailUtils.extractThumbnail(bitmap, bitmap.width, bitmap.height)
        val thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail)

        // Create thumbnail for the image which will also be sent to the backend
        thumbnailUris.add(thumbnailUri)
        currentPhotoLocation.add(bitmapUri)
    }
}
