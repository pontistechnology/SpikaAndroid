package com.clover.studio.exampleapp.ui.main.chat

import android.Manifest
import android.content.ContentResolver
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.JsonMessage
import com.clover.studio.exampleapp.data.models.entity.*
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.FragmentChatMessagesBinding
import com.clover.studio.exampleapp.ui.ImageSelectedContainer
import com.clover.studio.exampleapp.ui.ReactionsContainer
import com.clover.studio.exampleapp.ui.main.BlockedUsersFetchFailed
import com.clover.studio.exampleapp.ui.main.BlockedUsersFetched
import com.clover.studio.exampleapp.utils.CHUNK_SIZE
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Runnable
import timber.log.Timber

/*fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }
*/
private const val SCROLL_DISTANCE_NEGATIVE = -300
private const val SCROLL_DISTANCE_POSITIVE = 300
private const val MIN_HEIGHT_DIFF = 150
private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f

enum class UploadMimeTypes {
    IMAGE, VIDEO, FILE, MESSAGE
}

@AndroidEntryPoint
class ChatMessagesFragment : BaseFragment(), ChatOnBackPressed {
    private val viewModel: ChatViewModel by activityViewModels()
    private val args: ChatMessagesFragmentArgs by navArgs()
    private lateinit var roomWithUsers: RoomWithUsers
    private lateinit var bindingSetup: FragmentChatMessagesBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var detailsMessageAdapter: MessageDetailsAdapter
    private lateinit var messageReactionAdapter: MessageReactionAdapter
    private var messagesRecords: MutableList<MessageAndRecords> = mutableListOf()
    private var messageDetails: MutableList<MessageRecords> = ArrayList()
    private var unsentMessages: MutableList<Message> = ArrayList()

    private var currentMediaLocation: MutableList<Uri> = ArrayList()

    private var filesSelected: MutableList<Uri> = ArrayList()

    private var thumbnailUris: MutableList<Uri> = ArrayList()
    private var photoImageUri: Uri? = null
    private var isAdmin = false
    private var uploadIndex = 0
    private var progress = 0
    private var uploadPieces = 0L
    private var fileType: String = ""
    private var mediaType: UploadMimeTypes? = null
    private var tempMessageCounter = -1
    private var uploadInProgress = false
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetMessageActions: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetReplyAction: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetDetailsAction: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetReactionsAction: BottomSheetBehavior<ConstraintLayout>

    private var avatarFileId = 0L
    private var userName = ""
    private var firstEnter = true
    private var isEditing = false
    private var originalText = ""
    private var editedMessageId = 0
    private var messageBody: MessageBody? = null
    private lateinit var emojiPopup: EmojiPopup
    private lateinit var storagePermission: ActivityResultLauncher<String>
    private lateinit var storedMessage: Message
    private var oldPosition = 0
    private var scrollYDistance = 0
    private var sent = false
    private var heightDiff = 0
    private var exoPlayer: ExoPlayer? = null

    private var replyId: Long? = 0L

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
                    getImageOrVideo(photoImageUri!!)
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
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        bottomSheetBehaviour = BottomSheetBehavior.from(bindingSetup.bottomSheet.root)
        bottomSheetMessageActions = BottomSheetBehavior.from(bindingSetup.messageActions.root)
        bottomSheetReplyAction = BottomSheetBehavior.from(bindingSetup.replyAction.root)
        bottomSheetDetailsAction = BottomSheetBehavior.from(bindingSetup.detailsAction.root)
        bottomSheetReactionsAction = BottomSheetBehavior.from(bindingSetup.reactionsDetails.root)

        roomWithUsers = (activity as ChatScreenActivity?)!!.roomWithUsers!!
        emojiPopup = EmojiPopup(bindingSetup.root, bindingSetup.etMessage)

        // Check if we have left the room, if so, disable bottom message interaction
        if (roomWithUsers.room.roomExit == true) {
            bindingSetup.clRoomExit.visibility = View.VISIBLE
        } else {
            bindingSetup.clRoomExit.visibility = View.GONE
            checkStoragePermission()
            setUpAdapter()
            setUpMessageDetailsAdapter()
            initializeObservers()
            checkIsUserAdmin()
        }
        initViews()
        initListeners()

        return bindingSetup.root
    }

    private fun setAvatarAndName(avatarFileId: Long, userName: String) {
        bindingSetup.tvChatName.text = userName
        Glide.with(this)
            .load(avatarFileId.let { Tools.getFilePathUrl(it) })
            .placeholder(context?.getDrawable(R.drawable.img_user_placeholder))
            .into(bindingSetup.ivUserImage)
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
                    tempMessageCounter -= 1

                    // Delay next message sending by 2 seconds for better user experience.
                    // Could be removed if we deem it not needed.
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        uploadIndex += 1
                        if (currentMediaLocation.isNotEmpty()) {
                            if (uploadIndex < currentMediaLocation.size) {
                                if (Const.JsonFields.IMAGE_TYPE == fileType)
                                    uploadImage()
                                else if (Const.JsonFields.VIDEO_TYPE == fileType)
                                    uploadVideo()
                            } else
                                resetUploadFields()
                        } else if (filesSelected.isNotEmpty()) {
                            if (uploadIndex < filesSelected.size) uploadFile()
                            else resetUploadFields()
                        } else resetUploadFields()
                    }, 2000)
                }
                ChatStatesEnum.MESSAGE_SEND_FAIL -> Timber.d("Message send fail")
                else -> Timber.d("Other error")
            }
        })

        /*viewModel.getMessagesTimestampListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is MessagesTimestampFetched -> {
                    Timber.d("Messages timestamp fetched")
                    messages = it.messages as MutableList<Message>
                    //chatAdapter.submitList(it.messages)
                }
                is MessageTimestampFetchFail -> Timber.d("Failed to fetch messages timestamp")
                else -> Timber.d("Other error")
            }
        })*/

        viewModel.sendMessageDeliveredListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_DELIVERED -> {
                    Timber.d("Messages delivered")
                }
                ChatStatesEnum.MESSAGE_DELIVER_FAIL -> Timber.d("Failed to deliver messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getChatRoomAndMessageAndRecordsById(roomWithUsers.room.roomId)
            .observe(viewLifecycleOwner) {
                messagesRecords.clear()
                if (it.message?.isNotEmpty() == true) {
                    // Check if user can be blocked
                    if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
                        val containsElement =
                            it.message.any { message -> viewModel.getLocalUserId() == message.message.fromUserId }
                        if (containsElement) bindingSetup.clBlock.visibility = View.GONE
                        else bindingSetup.clBlock.visibility = View.VISIBLE
                    }

                    it.message.forEach { msg ->
                        messagesRecords.add(msg)
                    }
                    messagesRecords.sortByDescending { messages -> messages.message.createdAt }
                    // messagesRecords.toList -> for DiffUtil class
                    chatAdapter.submitList(messagesRecords.toList())

                    if (oldPosition != messagesRecords.size) {
                        showNewMessage()
                    }

                    if (firstEnter) {
                        oldPosition = messagesRecords.size
                        bindingSetup.rvChat.scrollToPosition(0)
                        firstEnter = false
                    }
                }
            }

        viewModel.fileUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is FilePieceUploaded -> {
                    try {
                        if (progress <= uploadPieces) {
                            updateUploadFileProgressBar(
                                progress + 1,
                                uploadPieces.toInt(),
                                fileType
                            )
                            progress++
                        } else progress = 0
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on piece")
                        handleUploadError()
                    }
                }

                is FileUploadVerified -> {
                    try {
                        requireActivity().runOnUiThread {
                            Timber.d("Successfully sent file")
                            if (it.fileId > 0) messageBody?.fileId = it.fileId
                            sendMessage(
                                fileType,
                                messageBody?.fileId!!,
                                0,
                                unsentMessages[uploadIndex].localId!!
                            )
                        }
                        // update room data
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on verify")
                        handleUploadError()
                    }
                }

                is FileUploadError -> {
                    try {
                        handleUploadError()
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on error")
                        handleUploadError()
                    }
                }

                else -> Timber.d("Other upload error")
            }
        })

        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
                if (it?.isNotEmpty() == true) {
                    viewModel.fetchBlockedUsersLocally(it)
                } else bindingSetup.clContactBlocked.visibility = View.GONE
            }

            viewModel.blockedListListener.observe(viewLifecycleOwner, EventObserver {
                when (it) {
                    is BlockedUsersFetched -> {
                        if (it.users.isNotEmpty()) {
                            val containsElement =
                                roomWithUsers.users.any { user -> it.users.find { blockedUser -> blockedUser.id == user.id } != null }
                            if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
                                if (containsElement) {
                                    bindingSetup.clContactBlocked.visibility = View.VISIBLE
                                } else bindingSetup.clContactBlocked.visibility = View.GONE
                            }
                        } else bindingSetup.clContactBlocked.visibility = View.GONE
                    }
                    BlockedUsersFetchFailed -> Timber.d("Failed to fetch blocked users")
                    else -> Timber.d("Other error")
                }
            })
        }

        viewModel.mediaUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is MediaPieceUploaded -> {
                    try {
                        if (progress <= uploadPieces) {
                            updateUploadProgressBar(progress + 1, uploadPieces.toInt())
                            progress++
                        } else progress = 0
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on piece")
                        handleUploadError()
                    }
                }

                is MediaUploadVerified -> {
                    try {
                        if (!it.isThumbnail) {
                            if (it.fileId > 0) messageBody?.fileId = it.fileId

                            sendMessage(
                                fileType,
                                messageBody?.fileId!!,
                                messageBody?.thumbId!!,
                                unsentMessages[uploadIndex].localId!!
                            )
                        } else {
                            if (it.thumbId > 0) messageBody?.thumbId = it.thumbId
                            if (Const.JsonFields.IMAGE_TYPE == fileType) {
                                messageBody?.let {
                                    uploadMedia(
                                        false,
                                        currentMediaLocation[uploadIndex],
                                        UploadMimeTypes.IMAGE
                                    )
                                }
                            } else {
                                messageBody?.let {
                                    uploadMedia(
                                        false,
                                        currentMediaLocation[uploadIndex],
                                        UploadMimeTypes.VIDEO
                                    )
                                }
                            }
                        }
                        // update room data
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on verified")
                        handleUploadError()
                    }
                }

                is MediaUploadError -> {
                    try {
                        requireActivity().runOnUiThread {
                            handleUploadError()
                        }
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on error")
                        handleUploadError()
                    }
                }

                else -> Timber.d("Other upload error")
            }
        })
    }

    private fun showNewMessage() {
        // If we send message
        if (sent) {
            scrollToPosition()
        } else {
            // If we received message and keyboard is open:
            if (heightDiff >= MIN_HEIGHT_DIFF && scrollYDistance > SCROLL_DISTANCE_POSITIVE) {
                scrollYDistance -= heightDiff
            }
            // We need to check where we are in recycler view:
            // If we are somewhere bottom
            if ((scrollYDistance <= 0) && (scrollYDistance > SCROLL_DISTANCE_NEGATIVE)
                || (scrollYDistance >= 0) && (scrollYDistance < SCROLL_DISTANCE_POSITIVE)
            ) {
                scrollToPosition()
            }
            // If we are somewhere up in chat, show new message dialog
            else {
                bindingSetup.cvNewMessages.visibility = View.VISIBLE
                val newMessages = messagesRecords.size - oldPosition
                if (newMessages == 1) {
                    bindingSetup.tvNewMessage.text =
                        getString(R.string.new_messages, newMessages.toString(), "")
                } else {
                    bindingSetup.tvNewMessage.text =
                        getString(R.string.new_messages, newMessages.toString(), "s")
                }
            }
        }
        sent = false
        return
    }

    private fun scrollToPosition() {
        oldPosition = messagesRecords.size
        bindingSetup.rvChat.smoothScrollToPosition(0)
        scrollYDistance = 0
    }

    private fun setUpAdapter() {
        exoPlayer = ExoPlayer.Builder(this.context!!).build()
        chatAdapter = ChatAdapter(
            context!!,
            viewModel.getLocalUserId()!!,
            roomWithUsers.users,
            exoPlayer!!,
            roomWithUsers.room.type,
            onMessageInteraction = { event, message ->
                if (bindingSetup.clContactBlocked.visibility != View.VISIBLE) {
                    run {
                        when (event) {
                            Const.UserActions.DOWNLOAD_FILE -> handleDownloadFile(message)
                            Const.UserActions.DOWNLOAD_CANCEL -> handleDownloadCancelFile(message)
                            Const.UserActions.MESSAGE_ACTION -> handleMessageAction(message)
                            Const.UserActions.MESSAGE_REPLY -> handleMessageReplyClick(message)
                            Const.UserActions.SHOW_MESSAGE_REACTIONS -> handleShowReactions(message)
                            else -> Timber.d("No other action currently")
                        }
                    }
                }
            }
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        bindingSetup.rvChat.itemAnimator = null
        bindingSetup.rvChat.adapter = chatAdapter
        layoutManager.stackFromEnd = true
        bindingSetup.rvChat.layoutManager = layoutManager
        // bindingSetup.rvChat.recycledViewPool.setMaxRecycledViews(0, 0)

        // Add callback for item swipe handling
        /*val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
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
                val position = viewHolder.absoluteAdapterPosition
                bindingSetup.etMessage.setText(messagesRecords[position].message.body?.text)
                chatAdapter.notifyItemChanged(position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(bindingSetup.rvChat)*/

        // Notify backend of messages seen
        viewModel.sendMessagesSeen(roomWithUsers.room.roomId)

        // Update room visited
        viewModel.updateRoomVisitedTimestamp(System.currentTimeMillis(), roomWithUsers.room.roomId)
    }

    private fun handleShowReactions(message: MessageAndRecords) {
        bindingSetup.vTransparent.visibility = View.VISIBLE
        bottomSheetReactionsAction.state = BottomSheetBehavior.STATE_EXPANDED

        setUpMessageReactionAdapter()

        // A temporary solution until we come up with a better way to store message records in the database
        // With this, we search in the list if there are reactions, sort them by the time of creation, and get one reaction for each user
        val reactionList =
            message.records?.filter { it.reaction != null }!!.sortedByDescending { it.createdAt }
                .distinctBy { it.userId }
        messageReactionAdapter.submitList(reactionList)

    }

    private fun handleMessageReplyClick(msg: MessageAndRecords) {
        val time = msg.message.body?.referenceMessage?.createdAt
        var position = -1
        for (messageRecord in messagesRecords) {
            position++
            if (messageRecord.message.createdAt == time) {
                break
            }
        }
        if (position != messagesRecords.size - 1) {
            bindingSetup.rvChat.scrollToPosition(position)
        }
    }

    private fun handleMessageAction(msg: MessageAndRecords) {
        val reactionsContainer = ReactionsContainer(this.context!!, null)
        bindingSetup.messageActions.reactionsContainer.addView(reactionsContainer)
        bottomSheetMessageActions.state = BottomSheetBehavior.STATE_EXPANDED
        bindingSetup.vTransparent.visibility = View.VISIBLE

        // For now, only show delete and edit for sender
        if (msg.message.senderMessage) {
            bindingSetup.messageActions.tvEdit.visibility = View.VISIBLE
            bindingSetup.messageActions.tvDelete.visibility = View.VISIBLE
        } else {
            bindingSetup.messageActions.tvEdit.visibility = View.GONE
            bindingSetup.messageActions.tvDelete.visibility = View.GONE
        }

        reactionsContainer.setButtonListener(object : ReactionsContainer.AddReaction {
            override fun addReaction(reaction: String) {
                if (reaction.isNotEmpty()) {
                    msg.message.reaction = reaction
                    addReaction(msg.message)
                    chatAdapter.notifyItemChanged(msg.message.messagePosition)
                    closeMessageSheet()
                }
            }
        })

        bindingSetup.messageActions.tvDelete.setOnClickListener {
            closeMessageSheet()
            showDeleteMessageDialog(msg.message)
        }

        bindingSetup.messageActions.tvEdit.setOnClickListener {
            closeMessageSheet()
            handleMessageEdit(msg.message)
        }

        bindingSetup.messageActions.tvReply.setOnClickListener {
            closeMessageSheet()
            bottomSheetReplyAction.state = BottomSheetBehavior.STATE_EXPANDED
            handleMessageReply(msg.message)
        }

        bindingSetup.messageActions.tvDetails.setOnClickListener {
            bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
            getDetailsList(msg.message)
            bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun handleDownloadCancelFile(message: MessageAndRecords) {
        // For now, message object is not necessary but maybe we can use it later
        showUploadError(getString(R.string.upload_file_in_progress))
    }

    private fun getDetailsList(detailsMessage: Message) {
        val myId = viewModel.getLocalUserId()
        for (message in messagesRecords) {
            if (message.message.id == detailsMessage.id) {
                for (record in message.records!!) {
                    if (record.userId != myId) {
                        if (record.type == Const.JsonFields.SEEN) {
                            messageDetails.add(record)
                        } else if (record.type == Const.JsonFields.DELIVERED) {
                            messageDetails.add(record)
                        }
                    }
                }
                break
            }
        }
        val sortedMessageDetails = messageDetails.sortedByDescending { it.type }
        val filteredList = sortedMessageDetails.distinctBy { it.userId }
        detailsMessageAdapter.submitList(ArrayList(filteredList))
        messageDetails.clear()
    }

    private fun setUpMessageDetailsAdapter() {
        detailsMessageAdapter = MessageDetailsAdapter(
            context!!,
            roomWithUsers,
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        bindingSetup.detailsAction.rvReactionsDetails.adapter = detailsMessageAdapter
        bindingSetup.detailsAction.rvReactionsDetails.layoutManager = layoutManager
        bindingSetup.detailsAction.rvReactionsDetails.itemAnimator = null
    }

    private fun setUpMessageReactionAdapter() {
        messageReactionAdapter = MessageReactionAdapter(
            context!!,
            roomWithUsers,
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        bindingSetup.reactionsDetails.rvReactionsDetails.adapter = messageReactionAdapter
        bindingSetup.reactionsDetails.rvReactionsDetails.layoutManager = layoutManager
        bindingSetup.reactionsDetails.rvReactionsDetails.itemAnimator = null
    }

    private fun closeMessageSheet() {
        bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
        bindingSetup.vTransparent.visibility = View.GONE
    }

    private fun addReaction(message: Message) {
        /*if (!reaction.clicked) {*/
        val jsonObject = JsonObject()
        jsonObject.addProperty(Const.Networking.MESSAGE_ID, message.id)
        jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.REACTION)
        jsonObject.addProperty(Const.JsonFields.REACTION, message.reaction)
        viewModel.sendReaction(jsonObject)
        /*} else {
            // Remove reaction
            val jsonObject = JsonObject()
            jsonObject.addProperty(Const.Networking.MESSAGE_ID, reaction.messageId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.REACTION)
            if (roomWithUsers.room.type == Const.JsonFields.PRIVATE){
                viewModel.deleteAllReactions(reaction.messageId)
            } else {
                viewModel.deleteReaction(reaction.reactionId, reaction.userId)
            }
        }*/
    }

    private fun initViews() {
        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            for (user in roomWithUsers.users) {
                if (user.id.toString() != viewModel.getLocalUserId().toString()) {
                    avatarFileId = user.avatarFileId!!
                    userName = user.displayName.toString()
                    break
                } else {
                    avatarFileId = user.avatarFileId!!
                    userName = user.displayName.toString()
                }
            }
        } else {
            avatarFileId = roomWithUsers.room.avatarFileId!!
            userName = roomWithUsers.room.name.toString()
        }
        setAvatarAndName(avatarFileId, userName)

        if (roomWithUsers.room.roomExit == true) {
            bindingSetup.ivVideoCall.setImageResource(R.drawable.img_video_call_disabled)
            bindingSetup.ivCallUser.setImageResource(R.drawable.img_call_user_disabled)
            bindingSetup.ivVideoCall.isEnabled = false
            bindingSetup.ivCallUser.isEnabled = false
        }

        bindingSetup.tvTitle.text = roomWithUsers.room.type

        bindingSetup.clBlock.setOnClickListener {
            val userIdToBlock =
                roomWithUsers.users.firstOrNull { user -> user.id != viewModel.getLocalUserId() }
            userIdToBlock?.let { idToBlock -> viewModel.blockUser(idToBlock.id) }
        }

        bindingSetup.tvUnblock.setOnClickListener {
            DialogError.getInstance(requireContext(),
                getString(R.string.unblock_user),
                getString(R.string.unblock_description, bindingSetup.tvChatName.text),
                getString(R.string.no),
                getString(R.string.unblock),
                object : DialogInteraction {
                    override fun onSecondOptionClicked() {
                        roomWithUsers.users.firstOrNull { user -> user.id != viewModel.getLocalUserId() }
                            ?.let { it1 -> viewModel.deleteBlockForSpecificUser(it1.id) }
                    }
                })
        }
    }

    private fun initListeners() {
        bindingSetup.clHeader.setOnClickListener {
            val action =
                ChatMessagesFragmentDirections.actionChatMessagesFragmentToChatDetailsFragment(
                    roomWithUsers.room.roomId,
                    isAdmin
                )
            findNavController().navigate(action)
        }

        bindingSetup.ivArrowBack.setOnClickListener {
            onBackArrowPressed()
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

        bindingSetup.ivBtnEmoji.setOnClickListener {
            emojiPopup.toggle() // Toggles visibility of the Popup.
            emojiPopup.dismiss() // Dismisses the Popup.
            emojiPopup.isShowing // Returns true when Popup is showing.
            bindingSetup.ivAdd.rotation = ROTATION_OFF
        }

        bindingSetup.etMessage.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            }
        }

        bindingSetup.rvChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                bindingSetup.rvChat.smoothScrollToPosition(0)
            }
        }

        bindingSetup.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scrollYDistance += dy
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // This condition checks if the RecyclerView is at the bottom
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    bindingSetup.cvNewMessages.visibility = View.GONE
                    oldPosition = messagesRecords.size
                    scrollYDistance = 0
                }
            }
        })


        bindingSetup.root.viewTreeObserver.addOnGlobalLayoutListener {
            heightDiff = bindingSetup.root.rootView.height - bindingSetup.root.height
        }

        bindingSetup.cvNewMessages.setOnClickListener {
            bindingSetup.rvChat.scrollToPosition(0)
            bindingSetup.cvNewMessages.visibility = View.GONE
            scrollYDistance = 0
            oldPosition = messagesRecords.size
        }

        bindingSetup.etMessage.addTextChangedListener {
            if (!isEditing) {
                if (it?.isNotEmpty() == true) {
                    showSendButton()
                    bindingSetup.ivAdd.rotation = ROTATION_OFF
                } else {
                    hideSendButton()
                }
            }
        }

        bindingSetup.ivButtonSend.setOnClickListener {
            val imageContainer = bindingSetup.llImagesContainer
            imageContainer.removeAllViews()
            if (currentMediaLocation.isNotEmpty()) {
                for (thumbnail in thumbnailUris) {
                    createTempMediaMessage(thumbnail)
                }
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (Const.JsonFields.IMAGE_TYPE == fileType) {
                        uploadImage()
                    } else {
                        uploadVideo()
                    }
                }, 2000)
            } else if (filesSelected.isNotEmpty()) {
                for (file in filesSelected) {
                    createTempFileMessage(file)
                }
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    uploadFile()
                }, 2000)
            } else {
                createTempTextMessage()
                sendMessage()
            }
            sent = true
            hideSendButton()
        }

        bindingSetup.messageActions.ivRemove.setOnClickListener {
            closeMessageSheet()
        }

        bindingSetup.reactionsDetails.ivRemove.setOnClickListener {
            bottomSheetReactionsAction.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bindingSetup.replyAction.ivRemove.setOnClickListener {
            if (bottomSheetReplyAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
                replyId = 0L
            }
        }

        bindingSetup.detailsAction.ivRemove.setOnClickListener {
            if (bottomSheetDetailsAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_COLLAPSED
                bindingSetup.vTransparent.visibility = View.GONE
            }
        }

        bindingSetup.ivAdd.setOnClickListener {
            if (bottomSheetReplyAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                replyId = 0L
                bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            if (!isEditing) {
                if (bottomSheetBehaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
                    bindingSetup.ivAdd.rotation = ROTATION_ON
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                    bindingSetup.vTransparent.visibility = View.VISIBLE
                }
            } else {
                resetEditingFields()
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

        bindingSetup.tvSave.setOnClickListener {
            editMessage()
            resetEditingFields()
        }

        // Bottom sheet listeners
        val bottomSheetBehaviorCallback =
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Ignore
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        bindingSetup.vTransparent.visibility = View.GONE
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        bindingSetup.vTransparent.visibility = View.VISIBLE
                    }

                }
            }

        val bottomSheetBehaviorCallbackMessageAction =
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Ignore
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (bottomSheetDetailsAction.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            bindingSetup.vTransparent.visibility = View.GONE
                        }
                    }
                    if (bottomSheetReplyAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            bindingSetup.vTransparent.visibility = View.VISIBLE
                        }
                    }
                }
            }

        bottomSheetMessageActions.addBottomSheetCallback(bottomSheetBehaviorCallbackMessageAction)
        bottomSheetDetailsAction.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetBehaviour.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetReplyAction.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetReactionsAction.addBottomSheetCallback(bottomSheetBehaviorCallback)
    }

    private fun resetEditingFields() {
        editedMessageId = 0
        isEditing = false
        originalText = ""
        bindingSetup.etMessage.setText("")
        bindingSetup.ivAdd.rotation = ROTATION_OFF
        bindingSetup.tvSave.visibility = View.GONE
        bindingSetup.ivCamera.visibility = View.VISIBLE
        bindingSetup.ivMicrophone.visibility = View.VISIBLE
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
        bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun showSendButton() {
        bindingSetup.ivCamera.visibility = View.INVISIBLE
        bindingSetup.ivMicrophone.visibility = View.INVISIBLE
        bindingSetup.ivButtonSend.visibility = View.VISIBLE
        bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = bindingSetup.ivButtonSend.id
        }
        bindingSetup.ivAdd.rotation = ROTATION_OFF
    }

    private fun showDeleteMessageDialog(message: Message) {
        ChooserDialog.getInstance(requireContext(),
            null,
            null,
            getString(R.string.delete_for_everyone),
            getString(R.string.delete_for_me),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    deleteMessage(message.id, Const.UserActions.DELETE_MESSAGE_ALL)
                }

                override fun onSecondOptionClicked() {
                    deleteMessage(message.id, Const.UserActions.DELETE_MESSAGE_ME)
                }
            })
    }

    private fun deleteMessage(messageId: Int, target: String) {
        viewModel.deleteMessage(messageId, target)
    }

    private fun handleMessageReply(message: Message) {
        bindingSetup.vTransparent.visibility = View.VISIBLE
        replyId = message.id.toLong()
        if (message.senderMessage) {
            bindingSetup.replyAction.clReplyContainer.background =
                context!!.getDrawable(R.drawable.bg_message_user)
        } else {
            bindingSetup.replyAction.clReplyContainer.background =
                context!!.getDrawable(R.drawable.bg_message_received)
        }

        for (user in roomWithUsers.users) {
            if (user.id == message.fromUserId) {
                bindingSetup.replyAction.tvUsername.text = user.displayName
                break
            }
        }

        when (message.type) {
            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                bindingSetup.replyAction.tvMessage.visibility = View.GONE
                bindingSetup.replyAction.ivReplyImage.visibility = View.VISIBLE
                val imagePath =
                    message.body?.fileId?.let { imagePath ->
                        Tools.getFilePathUrl(
                            imagePath
                        )
                    }
                if (Const.JsonFields.IMAGE_TYPE == message.type) {
                    bindingSetup.replyAction.tvReplyMedia.text = getString(
                        R.string.media,
                        context!!.getString(R.string.photo)
                    )
                    bindingSetup.replyAction.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_camera_reply,
                        0,
                        0,
                        0
                    )
                }
                if (Const.JsonFields.VIDEO_TYPE == message.type) {
                    bindingSetup.replyAction.tvReplyMedia.text = getString(
                        R.string.media,
                        context!!.getString(R.string.video)
                    )
                    bindingSetup.replyAction.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_video_reply,
                        0,
                        0,
                        0
                    )
                }
                bindingSetup.replyAction.tvReplyMedia.visibility = View.VISIBLE
                Glide.with(this)
                    .load(imagePath)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.img_image_placeholder)
                    .dontTransform()
                    .dontAnimate()
                    .into(bindingSetup.replyAction.ivReplyImage)
            }
            Const.JsonFields.AUDIO_TYPE -> {
                bindingSetup.replyAction.tvMessage.visibility = View.GONE
                bindingSetup.replyAction.tvReplyMedia.visibility = View.VISIBLE
                bindingSetup.replyAction.ivReplyImage.visibility = View.GONE
                bindingSetup.replyAction.tvReplyMedia.text =
                    getString(R.string.media, context!!.getString(R.string.audio))
                bindingSetup.replyAction.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_audio_reply,
                    0,
                    0,
                    0
                )
            }
            Const.JsonFields.FILE_TYPE -> {
                bindingSetup.replyAction.tvMessage.visibility = View.GONE
                bindingSetup.replyAction.ivReplyImage.visibility = View.GONE
                bindingSetup.replyAction.tvReplyMedia.visibility = View.VISIBLE
                bindingSetup.replyAction.tvReplyMedia.text =
                    getString(R.string.media, context!!.getString(R.string.file))
                bindingSetup.replyAction.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_file_reply,
                    0,
                    0,
                    0
                )
            }
            else -> {
                bindingSetup.replyAction.ivReplyImage.visibility = View.GONE
                bindingSetup.replyAction.tvReplyMedia.visibility = View.GONE
                bindingSetup.replyAction.tvMessage.visibility = View.VISIBLE
                val replyText = message.body?.text
                bindingSetup.replyAction.tvMessage.text = replyText
                bindingSetup.replyAction.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
            }
        }
    }

    private fun handleMessageEdit(message: Message) {
        isEditing = true
        originalText = message.body?.text.toString()
        editedMessageId = message.id
        bindingSetup.etMessage.setText(message.body?.text)
        bindingSetup.ivAdd.rotation = ROTATION_ON

        bindingSetup.etMessage.addTextChangedListener {
            if (isEditing) {
                if (!originalText.equals(it)) {
                    // Show save button
                    bindingSetup.tvSave.visibility = View.VISIBLE
                    bindingSetup.ivCamera.visibility = View.INVISIBLE
                    bindingSetup.ivMicrophone.visibility = View.INVISIBLE
                } else {
                    // Hide save button
                    bindingSetup.tvSave.visibility = View.GONE
                    bindingSetup.ivCamera.visibility = View.VISIBLE
                    bindingSetup.ivMicrophone.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun editMessage() {
        if (editedMessageId != 0) {
            val jsonObject = JsonObject()
            jsonObject.addProperty(
                Const.JsonFields.TEXT_TYPE,
                bindingSetup.etMessage.text.toString()
            )

            viewModel.editMessage(editedMessageId, jsonObject)
        }
    }

    private fun sendMessage() {
        sendMessage(
            messageFileType = Const.JsonFields.TEXT_TYPE,
            0,
            0,
            unsentMessages[tempMessageCounter].localId!!
        )
    }

    private fun sendMessage(
        messageFileType: String,
        fileId: Long,
        thumbId: Long,
        localId: String
    ) {
        val jsonMessage = JsonMessage(
            bindingSetup.etMessage.text.toString(),
            messageFileType,
            fileId,
            thumbId,
            roomWithUsers.room.roomId,
            localId,
            replyId
        )
        val jsonObject = jsonMessage.messageToJson()
        viewModel.sendMessage(jsonObject)

        if (replyId != 0L) {
            replyId = 0L
        }
    }

    private fun createTempTextMessage() {
        tempMessageCounter += 1
        messageBody = MessageBody(null, bindingSetup.etMessage.text.toString(), 1, 1, null, null)
        val tempMessage = Tools.createTemporaryMessage(
            tempMessageCounter,
            viewModel.getLocalUserId(),
            roomWithUsers.room.roomId,
            Const.JsonFields.TEXT_TYPE,
            messageBody!!
        )

        Timber.d("Temporary message: $tempMessage")
        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    /**
     * Method creates temporary file message which will be displayed to the user inside of the
     * chat adapter.
     *
     * @param uri Uri of the file being sent and with which the temporary message will be created.
     */
    private fun createTempFileMessage(uri: Uri) {
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

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(uri)!!,
            fileName
        )

        tempMessageCounter += 1
        messageBody = MessageBody(
            null,
            null,
            1,
            1,
            MessageFile(1, fileName, "", fileStream.length(), null, null),
            null,
        )

        val type = activity!!.contentResolver.getType(filesSelected[uploadIndex])!!
        fileType = if (type == Const.FileExtensions.AUDIO) {
            Const.JsonFields.AUDIO_TYPE
        } else {
            Const.JsonFields.FILE_TYPE
        }

        val tempMessage = Tools.createTemporaryMessage(
            tempMessageCounter,
            viewModel.getLocalUserId(),
            roomWithUsers.room.roomId,
            fileType,
            messageBody!!
        )
        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    /**
     * Creating temporary media message which will be shown to the user inside of the
     * chat adapter while the media file is uploading
     *
     * @param mediaUri uri of the media file for which a temporary image file will be created
     * which will hold the bitmap thumbnail.
     */
    private fun createTempMediaMessage(mediaUri: Uri) {
        tempMessageCounter += 1
        messageBody = MessageBody(
            null,
            null,
            1,
            1,
            MessageFile(
                1,
                "",
                "",
                0,
                null,
                mediaUri.toString()
            ),
            null,
        )

        // Media file is always thumbnail first. Therefore, we are sending CHAT_IMAGE as type
        val tempMessage = Tools.createTemporaryMessage(
            tempMessageCounter,
            viewModel.getLocalUserId(),
            roomWithUsers.room.roomId,
            Const.JsonFields.IMAGE_TYPE,
            messageBody!!
        )

        Timber.d("Temporary message: $tempMessage")
        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    private fun onBackArrowPressed() {
        if (uploadInProgress) {
            showUploadError(getString(R.string.upload_in_progress))
        } else {
            // Update room visited
            roomWithUsers.room.visitedRoom = System.currentTimeMillis()
            viewModel.updateRoomVisitedTimestamp(
                System.currentTimeMillis(),
                roomWithUsers.room.roomId
            )
            //
            activity!!.finish()
        }
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

    private fun uploadThumbnail(index: Int) {
        mediaType = UploadMimeTypes.IMAGE
        uploadMedia(true, thumbnailUris[index], mediaType!!)
    }

    private fun uploadVideoThumbnail(index: Int) {
        mediaType = UploadMimeTypes.VIDEO
        uploadMedia(true, thumbnailUris[index], mediaType!!)
    }

    private fun uploadImage() {
        messageBody = MessageBody(null, "", 0, 0, null, null)
        uploadThumbnail(uploadIndex)
    }

    private fun uploadVideo() {
        messageBody = MessageBody(null, "", 0, 0, null, null)
        uploadVideoThumbnail(uploadIndex)
    }

    /**
     * Method used for uploading files to the backend
     */
    private fun uploadFile() {
        uploadInProgress = true
        messageBody = MessageBody(null, "", 0, 0, null, null)
        val inputStream =
            activity!!.contentResolver.openInputStream(filesSelected[uploadIndex])

        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        val cr = activity!!.contentResolver
        cr.query(filesSelected[uploadIndex], projection, null, null, null)?.use { metaCursor ->
            if (metaCursor.moveToFirst()) {
                fileName = metaCursor.getString(0)
            }
        }

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(filesSelected[uploadIndex])!!,
            fileName
        )

        uploadPieces =
            if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                fileStream.length() / CHUNK_SIZE + 1
            else fileStream.length() / CHUNK_SIZE
        progress = 0

        val type = activity!!.contentResolver.getType(filesSelected[uploadIndex])!!
        fileType = if (Const.FileExtensions.AUDIO == type) {
            Const.JsonFields.AUDIO_TYPE
        } else {
            Const.JsonFields.FILE_TYPE
        }

        viewModel.uploadFile(
            requireActivity(),
            filesSelected[uploadIndex],
            uploadPieces,
            fileStream,
            fileType
        )
    }

    /**
     * One method used for uploading images and video files. They can be discerned by the
     * mediaType field send to the constructor.
     *
     * @param isThumbnail Declare if a thumbnail is being sent or a media file
     * @param uri Uri of the media/thumbnail file
     * @param mediaType Type of file being send to the server (Image, Video, Message, File)
     */
    private fun uploadMedia(
        isThumbnail: Boolean,
        uri: Uri,
        mediaType: UploadMimeTypes
    ) {
        uploadInProgress = true
        val inputStream =
            activity!!.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            activity!!.contentResolver.getType(uri)!!
        )
        uploadPieces =
            if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                fileStream.length() / CHUNK_SIZE + 1
            else fileStream.length() / CHUNK_SIZE
        progress = 0

        val fileType: String
        if (mediaType == UploadMimeTypes.IMAGE) {
            this.fileType = Const.JsonFields.IMAGE_TYPE
            fileType = Const.JsonFields.IMAGE_TYPE
        } else {
            fileType = if (isThumbnail) {
                Const.JsonFields.IMAGE_TYPE
            } else {
                Const.JsonFields.VIDEO_TYPE
            }
            this.fileType = Const.JsonFields.VIDEO_TYPE
        }

        viewModel.uploadMedia(
            requireActivity(),
            uri,
            fileType,
            uploadPieces,
            fileStream,
            isThumbnail
        )
    }

    /**
     * Reset upload fields and clear local cache on success or critical fail
     */
    private fun resetUploadFields() {
        if (tempMessageCounter >= -1) {
            viewModel.deleteLocalMessages(unsentMessages)
            tempMessageCounter = -1
        }

        uploadIndex = 0
        currentMediaLocation.clear()
        filesSelected.clear()
        thumbnailUris.clear()
        uploadInProgress = false
        unsentMessages.clear()
        context?.cacheDir?.deleteRecursively()
    }

    /**
     * Method handles error for specific files and checks if it should continue uploading other
     * files waiting in row, if there are any.
     *
     * Also displays a toast message for failed uploads.
     */
    private fun handleUploadError() {
        uploadIndex += 1

        tempMessageCounter -= 1

        if (currentMediaLocation.isNotEmpty()) {
            if (uploadIndex < currentMediaLocation.size) {
                if (Const.JsonFields.IMAGE_TYPE == fileType) {
                    uploadImage()
                } else {
                    uploadVideo()
                }
            } else {
                resetUploadFields()
            }
        } else if (filesSelected.isNotEmpty()) {
            if (uploadIndex < filesSelected.size) {
                uploadFile()
            } else {
                resetUploadFields()
            }
        } else resetUploadFields()

        Toast.makeText(
            activity!!.baseContext,
            getString(R.string.failed_file_upload),
            Toast.LENGTH_SHORT
        ).show()
        uploadInProgress = false
    }

    private fun showUploadError(errorMessage: String) {
        DialogError.getInstance(activity!!,
            getString(R.string.warning),
            errorMessage,
            getString(R.string.back),
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    // ignore
                }

                override fun onSecondOptionClicked() {
                    // Update room visited
                    roomWithUsers.room.visitedRoom = System.currentTimeMillis()
                    viewModel.updateRoomVisitedTimestamp(
                        System.currentTimeMillis(),
                        roomWithUsers.room.roomId
                    )
                    if (unsentMessages.isNotEmpty()) {
                        viewModel.deleteLocalMessages(unsentMessages)
                    }
                    activity!!.finish()
                }
            })
    }

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
                Timber.d("Files selected 1: $filesSelected")
                filesSelected.removeAt(bindingSetup.llImagesContainer.indexOfChild(imageSelected))
                Timber.d("Files selected 2: $filesSelected")
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })
        bindingSetup.llImagesContainer.addView(imageSelected)
    }

    private fun getImageOrVideo(uri: Uri) {
        val cR: ContentResolver = context!!.contentResolver
        val mime = cR.getType(uri)

        if (mime?.contains(Const.JsonFields.VIDEO_TYPE) == true) {
            fileType = Const.JsonFields.VIDEO_TYPE
            convertVideo(uri)
        } else {
            fileType = Const.JsonFields.IMAGE_TYPE
            convertImageToBitmap(uri)
        }
    }

    private fun convertVideo(videoUri: Uri) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, videoUri)
        val bitmap = mmr.frameAtTime

        val imageSelected = ImageSelectedContainer(activity!!, null)
        bitmap.let { imageBitmap -> imageSelected.setImage(imageBitmap!!) }
        bindingSetup.llImagesContainer.addView(imageSelected)

        activity!!.runOnUiThread { showSendButton() }
        imageSelected.setButtonListener(object :
            ImageSelectedContainer.RemoveImageSelected {
            override fun removeImage() {
                Timber.d("Media selected 1: $currentMediaLocation")
                thumbnailUris.removeAt(bindingSetup.llImagesContainer.indexOfChild(imageSelected))
                currentMediaLocation.removeAt(
                    bindingSetup.llImagesContainer.indexOfChild(
                        imageSelected
                    )
                )
                Timber.d("Media selected 2: $currentMediaLocation")
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })
        val thumbnail =
            ThumbnailUtils.extractThumbnail(bitmap, bitmap!!.width, bitmap.height)
        val thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail)

        thumbnailUris.add(thumbnailUri)
        currentMediaLocation.add(videoUri)
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
                Timber.d("Media selected 1: $currentMediaLocation")
                thumbnailUris.removeAt(bindingSetup.llImagesContainer.indexOfChild(imageSelected))
                currentMediaLocation.removeAt(
                    bindingSetup.llImagesContainer.indexOfChild(
                        imageSelected
                    )
                )
                Timber.d("Media selected 2: $currentMediaLocation")
                bindingSetup.llImagesContainer.removeView(imageSelected)
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        })
        val thumbnail =
            ThumbnailUtils.extractThumbnail(bitmap, bitmap.width, bitmap.height)
        val thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail)

        // Create thumbnail for the image which will also be sent to the backend
        thumbnailUris.add(thumbnailUri)
        currentMediaLocation.add(bitmapUri)
    }

    private fun checkStoragePermission() {
        storagePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    context?.let { context -> Tools.downloadFile(context, storedMessage) }
                } else {
                    Timber.d("Couldn't download file. No permission granted.")
                }
            }
    }

    private fun handleDownloadFile(message: MessageAndRecords) {
        when {
            context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } == PackageManager.PERMISSION_GRANTED -> {
                Tools.downloadFile(context!!, message.message)
            }

            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                // TODO show why permission is needed
            }

            else -> {
                storedMessage = message.message
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

    }

    /**
     * Update progress bar in recycler view
     * get viewHolder from position and progress bar from that viewHolder
     *  we are rapidly updating progressbar so we didn't use notify method as it always update whole row instead of only progress bar
     *  @param progress : new progress value
     */
    private fun updateUploadProgressBar(progress: Int, maxProgress: Int) {

        Timber.d("Temp message position: $tempMessageCounter")
        val viewHolder = bindingSetup.rvChat.findViewHolderForAdapterPosition(tempMessageCounter)

        Timber.d("Setting max progress $maxProgress")
        (viewHolder as ChatAdapter.SentMessageHolder).binding.progressBar.max = maxProgress

        Timber.d("Updating with progress $progress")
        viewHolder.binding.progressBar.secondaryProgress = progress
    }

    private fun updateUploadFileProgressBar(progress: Int, maxProgress: Int, type: String) {
        val viewHolder = bindingSetup.rvChat.findViewHolderForAdapterPosition(tempMessageCounter)
        if (Const.JsonFields.AUDIO_TYPE == type) {
            (viewHolder as ChatAdapter.SentMessageHolder).binding.pbAudio.visibility = View.VISIBLE
            viewHolder.binding.ivCancelAudio.visibility = View.VISIBLE
            viewHolder.binding.ivPlayAudio.visibility = View.GONE
            viewHolder.binding.pbAudio.max = maxProgress
            viewHolder.binding.pbAudio.secondaryProgress = progress
        } else {
            (viewHolder as ChatAdapter.SentMessageHolder).binding.pbFile.visibility = View.VISIBLE
            viewHolder.binding.ivCancelFile.visibility = View.VISIBLE
            viewHolder.binding.ivDownloadFile.visibility = View.GONE
            viewHolder.binding.pbFile.max = maxProgress
            viewHolder.binding.pbFile.secondaryProgress = progress
        }
    }

    override fun onBackPressed(): Boolean {
        return if (uploadInProgress) {
            showUploadError(getString(R.string.upload_in_progress))
            false
        } else true
    }

    override fun onResume() {
        super.onResume()
        firstEnter = args.scrollDown
        bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
        viewModel.getBlockedUsersList()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exoPlayer != null) {
            exoPlayer!!.release()
        }
        viewModel.unregisterSharedPrefsReceiver()
    }
}