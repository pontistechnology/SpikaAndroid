package com.clover.studio.spikamessenger.ui.main.chat

import android.Manifest
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.FileMetadata
import com.clover.studio.spikamessenger.data.models.JsonMessage
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentChatMessagesBinding
import com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets.ChatBottomSheet
import com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets.DetailsBottomSheet
import com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets.MediaBottomSheet
import com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets.ReactionsBottomSheet
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.MessageSwipeController
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getFileMimeType
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.FilesHelper
import com.clover.studio.spikamessenger.utils.helpers.FilesHelper.getUniqueRandomId
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.UploadService
import com.google.gson.JsonObject
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File


private const val SCROLL_DISTANCE_NEGATIVE = -300
private const val SCROLL_DISTANCE_POSITIVE = 300
private const val MIN_HEIGHT_DIFF = 150
private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f
private const val NEW_MESSAGE_ANIMATION_DURATION = 300L

data class TempUri(
    val uri: Uri,
    val type: String,
)

@AndroidEntryPoint
class ChatMessagesFragment : BaseFragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var bindingSetup: FragmentChatMessagesBinding

    private var messageSearchId: Int? = 0

    private var roomWithUsers: RoomWithUsers? = null
    private var user: User? = null
    private var messagesRecords: MutableList<MessageAndRecords> = mutableListOf()
    private var unsentMessages: MutableList<Message> = ArrayList()
    private lateinit var storedMessage: Message
    private var localUserId: Int = 0

    private lateinit var chatAdapter: ChatAdapter
    var itemTouchHelper: ItemTouchHelper? = null
    private var valueAnimator: ValueAnimator? = null

    private var currentMediaLocation: MutableList<Uri> = ArrayList()
    private var filesSelected: MutableList<Uri> = ArrayList()
    private var thumbnailUris: MutableList<Uri> = ArrayList()
    private var tempFilesToCreate: MutableList<TempUri> = ArrayList()
    private var uploadFiles: ArrayList<FileData> = ArrayList()
    private var selectedFiles: MutableList<Uri> = ArrayList()
    private var uriPairList: MutableList<Pair<Uri, Uri>> = mutableListOf()

    private lateinit var fileUploadService: UploadService

    private var photoImageUri: Uri? = null
    private var isFetching = false
    private var directory: File? = null

    private lateinit var storagePermission: ActivityResultLauncher<String>
    private var exoPlayer: ExoPlayer? = null

    private var shouldScroll: Boolean = false
    private var listState: Parcelable? = null
    private var scrollYDistance = 0
    private var heightDiff = 0
    private var scrollToPosition = 0

    private var avatarFileId = 0L
    private var userName = ""

    private var isEditing = false
    private var originalText = ""
    private var editedMessageId = 0
    private var replyId: Long? = 0L

    private lateinit var emojiPopup: EmojiPopup

    private var navOptionsBuilder: NavOptions? = null

    private val chooseFileContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (!it.isNullOrEmpty()) {
                for (uri in it) {
                    selectedFiles.add(uri)
                    activity?.contentResolver?.takePersistableUriPermission(
                        uri,
                        FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                handleUserSelectedFile(selectedFiles)
            }
        }

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (!it.isNullOrEmpty()) {
                for (uri in it) {
                    selectedFiles.add(uri)
                    activity?.contentResolver?.takePersistableUriPermission(
                        uri,
                        FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                handleUserSelectedFile(selectedFiles)
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                if (photoImageUri != null) {
                    selectedFiles.add(photoImageUri!!)
                    handleUserSelectedFile(selectedFiles)
                } else {
                    Timber.d("Photo error")
                }
            } else Timber.d("Photo error")
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
            BuildConfig.APPLICATION_ID + ".fileprovider",
            Tools.createImageFile(
                (activity!!)
            )
        )
        takePhotoContract.launch(photoImageUri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        bindingSetup = FragmentChatMessagesBinding.inflate(layoutInflater)

        postponeEnterTransition()

        roomWithUsers = if (viewModel.roomWithUsers.value != null) {
            viewModel.roomWithUsers.value
        } else {
            activity?.intent!!.getParcelableExtra(Const.IntentExtras.ROOM_ID_EXTRA)
        }

        emojiPopup = EmojiPopup(bindingSetup.root, bindingSetup.etMessage)
        navOptionsBuilder = Tools.createCustomNavOptions()

        return bindingSetup.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (listState != null) {
            shouldScroll = true
        }

        localUserId = viewModel.getLocalUserId()!!
        messageSearchId = viewModel.searchMessageId.value

        setUpAdapter()
        initializeObservers()
        initViews()
        initListeners()
        checkStoragePermission()
    }

    private fun initViews() = with(bindingSetup) {
        directory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        clRoomExit.visibility =
            if (roomWithUsers?.room?.roomExit == true || roomWithUsers?.room?.deleted == true) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (Const.JsonFields.PRIVATE == roomWithUsers?.room?.type) {
            user =
                roomWithUsers?.users?.firstOrNull { user -> user.id.toString() != localUserId.toString() }
            avatarFileId = user?.avatarFileId ?: 0
            userName = user?.formattedDisplayName.toString()
            chatHeader.tvTitle.text = user?.telephoneNumber
        } else {
            avatarFileId = roomWithUsers?.room?.avatarFileId ?: 0
            userName = roomWithUsers?.room?.name.toString()
            chatHeader.tvTitle.text =
                roomWithUsers?.users?.size.toString() + getString(R.string.members)
        }

        setAvatarAndName(avatarFileId, userName)

        // Clear notifications for this room
        roomWithUsers?.room?.roomId?.let {
            NotificationManagerCompat.from(requireContext())
                .cancel(it)
        }
    }

    private fun setAvatarAndName(avatarFileId: Long, userName: String) =
        with(bindingSetup.chatHeader) {
            tvChatName.text = userName
            Glide.with(this@ChatMessagesFragment)
                .load(avatarFileId.let { Tools.getFilePathUrl(it) })
                .placeholder(
                    AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.img_user_placeholder
                    )
                )
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivUserImage)
        }

    private fun initListeners() = with(bindingSetup) {
        chatHeader.clHeader.setOnClickListener {
            if (Const.JsonFields.PRIVATE == roomWithUsers?.room?.type) {
                val bundle =
                    bundleOf(
                        Const.Navigation.USER_PROFILE to roomWithUsers?.users!!.firstOrNull { user -> user.id != localUserId },
                        Const.Navigation.ROOM_ID to roomWithUsers?.room!!.roomId,
                    )
                findNavController().navigate(
                    R.id.action_chatMessagesFragment_to_contactDetailsFragment,
                    bundle,
                    navOptionsBuilder
                )
            } else {
                val action =
                    ChatMessagesFragmentDirections.actionChatMessagesFragmentToChatDetailsFragment(
                        roomWithUsers = roomWithUsers!!
                    )
                findNavController().navigate(action, navOptionsBuilder)
            }
        }

        chatHeader.ivArrowBack.setOnClickListener {
            onBackArrowPressed()
        }

        ivCamera.setOnClickListener {
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

        ivBtnEmoji.setOnClickListener {
            emojiPopup.toggle()
            emojiPopup.dismiss()
            ivAdd.rotation = ROTATION_OFF
        }

        etMessage.setOnClickListener {
            if (emojiPopup.isShowing) emojiPopup.dismiss()
        }

        // This listener is for keyboard opening
        rvChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                if (sendingScrollVisibility()) {
                    rvChat.smoothScrollToPosition(0)
                }
            }
        }

        root.viewTreeObserver.addOnGlobalLayoutListener {
            heightDiff = root.rootView.height - root.height
        }

        cvNewMessages.setOnClickListener {
            rvChat.scrollToPosition(0)
            cvNewMessages.visibility = View.INVISIBLE
            scrollYDistance = 0
            viewModel.clearMessages()
        }

        cvBottomArrow.setOnClickListener {
            rvChat.scrollToPosition(0)
            cvBottomArrow.visibility = View.INVISIBLE
            scrollYDistance = 0
        }

        etMessage.addTextChangedListener {
            if (!isEditing) {
                if (it?.isNotEmpty() == true) {
                    showSendButton()
                    ivAdd.rotation = ROTATION_OFF
                } else {
                    hideSendButton()
                }
            }
        }

        ivButtonSend.setOnClickListener {
            vTransparent.visibility = View.GONE
            if (etMessage.text?.trim().toString().isNotEmpty()) {
                createTempTextMessage()
                sendMessage()
            }
            etMessage.setText("")
            hideSendButton()
            replyAction.root.visibility = View.GONE
        }

        tvUnblock.setOnClickListener {
            DialogError.getInstance(requireContext(),
                getString(R.string.unblock_user),
                getString(
                    R.string.unblock_description,
                    chatHeader.tvChatName.text
                ),
                getString(R.string.no),
                getString(R.string.unblock),
                object : DialogInteraction {
                    override fun onSecondOptionClicked() {
                        roomWithUsers?.users!!.firstOrNull { user -> user.id != localUserId }
                            ?.let { user -> viewModel.deleteBlockForSpecificUser(user.id) }
                    }
                })
        }

        tvSave.setOnClickListener {
            editMessage()
            resetEditingFields()
        }

        replyAction.ivRemove.setOnClickListener {
            replyAction.root.visibility = View.GONE
            replyId = 0L
        }

        ivAdd.setOnClickListener {
            if (!isEditing) {
                val mediaBottomSheet = MediaBottomSheet(requireContext())
                mediaBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    MediaBottomSheet.TAG
                )
                mediaBottomSheet.setActionListener(object :
                    MediaBottomSheet.BottomSheetMediaAAction {
                    override fun chooseFileAction() {
                        chooseFile()
                    }
                })
            } else {
                resetEditingFields()
            }
        }
    }

    private fun initializeObservers() {
        viewModel.messageSendListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (unsentMessages.isNotEmpty()) {
                        val message =
                            unsentMessages.find { msg -> msg.localId == it.responseData?.data?.message?.localId }
                        unsentMessages.remove(message)
                    }
                }

                Resource.Status.ERROR -> Timber.d("Message send fail: $it")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessageAndRecords(roomId = roomWithUsers!!.room.roomId)
            .observe(viewLifecycleOwner) {
                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        messagesRecords.clear()
                        if (it.responseData?.isNotEmpty() == true) {
                            it.responseData.forEach { msg ->
                                messagesRecords.add(msg)
                            }

                            chatAdapter.submitList(messagesRecords.toList())
                            updateSwipeController()

                            if (listState == null && scrollYDistance == 0) {
                                bindingSetup.rvChat.scrollToPosition(0)
                            }
                        } else chatAdapter.submitList(messagesRecords.toList())

                        (view?.parent as? ViewGroup)?.doOnPreDraw {
                            startPostponedEnterTransition()
                        }

                        if (listState != null && shouldScroll) {
                            bindingSetup.rvChat.layoutManager?.onRestoreInstanceState(listState)
                            shouldScroll = false
                        }

                        // If messageSearchId is not 0, it means the user navigated via message
                        // search. For now, we will just fetch next sets of data until we find
                        // the correct message id in the adapter to navigate to.
                        if (messageSearchId != 0) {
                            if (messagesRecords.firstOrNull { messageAndRecords -> messageAndRecords.message.id == messageSearchId } != null) {
                                val position =
                                    messagesRecords.indexOfFirst { messageAndRecords -> messageAndRecords.message.id == messageSearchId }
                                scrollToPosition = position
                                if (position != -1) {
                                    bindingSetup.rvChat.smoothScrollToPosition(position)
                                }

                                messageSearchId = 0
                                viewModel.searchMessageId.value = 0
                            } else {
                                viewModel.fetchNextSet(roomWithUsers?.room!!.roomId)
                            }
                        }
                    }

                    Resource.Status.LOADING -> Timber.d("Message get loading")
                    else -> Timber.d("Message get error")
                }
            }

        viewModel.messagesReceived.observe(viewLifecycleOwner) { messages ->
            val receivedMessages = messages.filter {
                it.roomId == roomWithUsers?.room!!.roomId
                        && it.fromUserId != localUserId
            }
            if (receivedMessages.isNotEmpty()) {
                showNewMessage(receivedMessages.size)
                // Notify backend of messages seen
                viewModel.sendMessagesSeen(roomWithUsers?.room!!.roomId)
            }
        }

        if (Const.JsonFields.PRIVATE == roomWithUsers?.room?.type) {
            viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
                if (it?.isNotEmpty() == true) {
                    viewModel.fetchBlockedUsersLocally(it)
                }
            }

            viewModel.blockedListListener.observe(viewLifecycleOwner, EventObserver {
                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        Timber.d("Blocked users fetched successfully")
                        if (it.responseData != null) {
                            val containsElement =
                                roomWithUsers!!.users.any { user -> it.responseData.find { blockedUser -> blockedUser.id == user.id } != null }
                            if (Const.JsonFields.PRIVATE == roomWithUsers!!.room.type) {
                                if (containsElement) {
                                    bindingSetup.llContactBlocked.visibility = View.VISIBLE
                                } else bindingSetup.llContactBlocked.visibility = View.GONE
                            }
                        } else bindingSetup.llContactBlocked.visibility = View.GONE
                    }

                    Resource.Status.ERROR -> {
                        Timber.d("Failed to fetch blocked users")
                        context?.let { context ->
                            DialogError.getInstance(
                                context,
                                getString(R.string.error),
                                it.message,
                                null,
                                getString(R.string.ok),
                                object : DialogInteraction {
                                    override fun onSecondOptionClicked() {
                                        // Ignore
                                    }
                                })
                        }
                    }

                    else -> Timber.d("Other error")
                }
            })
        }

        viewModel.roomInfoUpdated.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData?.data?.room?.roomId == roomWithUsers!!.room.roomId) {
                        val avatarFile = it.responseData.data.room.avatarFileId ?: 0L
                        val roomName = it.responseData.data.room.name ?: ""
                        roomWithUsers!!.room.apply {
                            avatarFileId = avatarFile
                            name = roomName
                        }
                        setAvatarAndName(avatarFile, roomName)
                    }
                }

                Resource.Status.ERROR -> Timber.d("Error while updating room data")
                else -> Timber.d("Other error")
            }
        })

        viewModel.sendMessagesSeen(roomId = roomWithUsers!!.room.roomId)
        viewModel.updateUnreadCount(roomId = roomWithUsers!!.room.roomId)
    }


    private fun showNewMessage(messagesSize: Int) = with(bindingSetup) {
        valueAnimator?.end()
        valueAnimator?.removeAllUpdateListeners()
        cvBottomArrow.visibility = View.INVISIBLE

        // If we received message and keyboard is open:
        if (heightDiff >= MIN_HEIGHT_DIFF && scrollYDistance > SCROLL_DISTANCE_POSITIVE) {
            scrollYDistance -= heightDiff
        }

        if (!(sendingScrollVisibility())) {
            cvNewMessages.visibility = View.VISIBLE

            if (messagesSize == 1 && cvNewMessages.visibility == View.INVISIBLE) {
                tvNewMessage.text =
                    getString(R.string.new_messages, messagesSize.toString(), "").trim()

                val startWidth = ivBottomArrow.width
                val endWidth =
                    (tvNewMessage.width + bindingSetup.ivBottomArrow.width)

                valueAnimator = ValueAnimator.ofInt(startWidth, endWidth).apply {
                    addUpdateListener { valueAnimator ->
                        val layoutParams = cvNewMessages.layoutParams
                        layoutParams.width = valueAnimator.animatedValue as Int
                        cvNewMessages.layoutParams = layoutParams
                    }
                    duration = NEW_MESSAGE_ANIMATION_DURATION
                }
                valueAnimator?.start()

            } else {
                tvNewMessage.text =
                    getString(R.string.new_messages, messagesSize.toString(), "s").trim()
            }
        } else if (messagesSize > 0) {
            rvChat.scrollToPosition(0)
            viewModel.clearMessages()
        }
        return@with
    }

    private fun sendingScrollVisibility(): Boolean {
        return ((scrollYDistance <= 0) && (scrollYDistance > SCROLL_DISTANCE_NEGATIVE) || (scrollYDistance >= 0) && (scrollYDistance < SCROLL_DISTANCE_POSITIVE))
    }

    private fun showBottomArrow() = with(bindingSetup) {
        if (heightDiff >= MIN_HEIGHT_DIFF && scrollYDistance > SCROLL_DISTANCE_POSITIVE) {
            scrollYDistance -= heightDiff
        }
        // If we are somewhere up
        if (!(sendingScrollVisibility())) {
            if (cvNewMessages.visibility == View.VISIBLE) {
                cvBottomArrow.visibility = View.INVISIBLE
            } else {
                cvBottomArrow.visibility = View.VISIBLE
            }
        } else {
            cvBottomArrow.visibility = View.INVISIBLE
        }
    }

    private fun setUpAdapter() = with(bindingSetup) {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()

        chatAdapter = ChatAdapter(
            requireContext(),
            localUserId,
            roomWithUsers!!.users,
            exoPlayer!!,
            roomWithUsers!!.room.type,
            onMessageInteraction = { event, message ->
                if (!roomWithUsers!!.room.deleted && !roomWithUsers!!.room.roomExit) {
                    run {
                        when (event) {
                            Const.UserActions.DOWNLOAD_FILE -> handleDownloadFile(message)
                            Const.UserActions.DOWNLOAD_CANCEL -> handleDownloadCancelFile(message.message)
                            Const.UserActions.MESSAGE_ACTION -> handleMessageAction(message)
                            Const.UserActions.MESSAGE_REPLY -> handleMessageReplyClick(message)
                            Const.UserActions.RESEND_MESSAGE -> handleMessageResend(message)
                            Const.UserActions.SHOW_MESSAGE_REACTIONS -> handleShowReactions(message)
                            Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT -> handleMediaNavigation(
                                message
                            )

                            else -> Timber.d("No other action currently")
                        }
                    }
                }
            }
        )

        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        rvChat.apply {
            adapter = chatAdapter
            itemAnimator = null
            linearLayoutManager.stackFromEnd = true
            layoutManager = linearLayoutManager
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    scrollYDistance += dy

                    val lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition()
                    val totalItemCount = linearLayoutManager.itemCount
                    if (lastVisiblePosition == totalItemCount - 1 && !isFetching) {
                        Timber.d("Fetching next batch of data")
                        viewModel.fetchNextSet(roomWithUsers!!.room.roomId)
                        isFetching = true
                    } else if (lastVisiblePosition != totalItemCount - 1) {
                        isFetching = false // Reset the flag when user scrolls away from the bottom
                    }
                    showBottomArrow()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    // This condition checks if the RecyclerView is at the bottom
                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        cvNewMessages.visibility = View.INVISIBLE
                        cvBottomArrow.visibility = View.INVISIBLE
                        scrollYDistance = 0
                        viewModel.clearMessages()
                    }
                }
            })
        }

        setUpAdapterDataObserver()
    }

    private fun setUpAdapterDataObserver() = with(bindingSetup) {
        val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val scrollingIndex = positionStart + itemCount - 1
                if (scrollingIndex >= 0 && chatAdapter.currentList.getOrNull(scrollingIndex)?.message?.fromUserId == localUserId) {
                    if (sendingScrollVisibility()) {
                        rvChat.scrollToPosition(0)
                        scrollYDistance = 0
                        cvBottomArrow.visibility = View.INVISIBLE
                    } else {
                        cvBottomArrow.visibility = View.VISIBLE
                    }
                }
            }
        }
        chatAdapter.registerAdapterDataObserver(adapterDataObserver)
    }

    private fun updateSwipeController() {
        itemTouchHelper?.attachToRecyclerView(null)
        val messageSwipeController =
            MessageSwipeController(
                context!!,
                messagesRecords,
                onSwipeAction = { action, position ->
                    when (action) {
                        Const.UserActions.ACTION_RIGHT -> {
                            bindingSetup.replyAction.root.visibility = View.VISIBLE
                            handleMessageReply(messagesRecords[position].message)
                        }

                        Const.UserActions.ACTION_LEFT -> {
                            messagesRecords.find { it.message.id == messagesRecords[position].message.id }
                                ?.let {
                                    DetailsBottomSheet(
                                        context = requireContext(),
                                        roomWithUsers = roomWithUsers!!,
                                        messagesRecords = it,
                                        localUserId = localUserId
                                    )
                                }?.show(
                                    requireActivity().supportFragmentManager,
                                    DetailsBottomSheet.TAG
                                )
                        }
                    }
                })

        itemTouchHelper = ItemTouchHelper(messageSwipeController)
        if (!roomWithUsers!!.room.deleted && !roomWithUsers!!.room.roomExit) itemTouchHelper!!.attachToRecyclerView(
            bindingSetup.rvChat
        )
    }

    private fun handleMessageReplyClick(msg: MessageAndRecords) {
        val position =
            messagesRecords.indexOfFirst { it.message.createdAt == msg.message.body?.referenceMessage?.createdAt }
        if (position != -1) {
            bindingSetup.rvChat.scrollToPosition(position)
        }
    }

    private fun handleMessageResend(message: MessageAndRecords) {
        ChooserDialog.getInstance(requireContext(),
            getString(R.string.resend),
            null,
            getString(R.string.resend_message),
            "",
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    resendMessage(message.message)
                }

                override fun onSecondOptionClicked() {
                    // Ignore
                }
            })
    }

    private fun handleShowReactions(msg: MessageAndRecords) {
        val reactionsBottomSheet =
            ReactionsBottomSheet(requireContext(), msg, roomWithUsers!!, localUserId)
        reactionsBottomSheet.setReactionListener(object : ReactionsBottomSheet.ReactionsAction {
            override fun deleteReaction(reaction: MessageRecords?) {
                reaction?.id?.let { viewModel.deleteReaction(it) }
            }
        })
        reactionsBottomSheet.show(
            requireActivity().supportFragmentManager,
            ReactionsBottomSheet.TAG
        )
    }

    private fun handleMessageAction(msg: MessageAndRecords) = with(bindingSetup) {
        if (msg.message.deleted == null || msg.message.deleted == true) {
            return
        }

        hideKeyboard(root)

        val bottomSheet = ChatBottomSheet(msg.message, localUserId)
        bottomSheet.setActionListener(object : ChatBottomSheet.BottomSheetAction {
            override fun actionCopy() {
                handleMessageCopy(msg.message)
            }

            override fun actionClose() {
                rotationAnimation()
            }

            override fun actionDelete() {
                showDeleteMessageDialog(msg.message)
            }

            override fun actionEdit() {
                handleMessageEdit(msg.message)
            }

            override fun actionReply() {
                replyAction.root.visibility = View.VISIBLE
                handleMessageReply(msg.message)
            }

            override fun actionDetails() {
                messagesRecords.find { it.message.id == msg.message.id }?.let {
                    DetailsBottomSheet(
                        context = requireContext(),
                        roomWithUsers = roomWithUsers!!,
                        messagesRecords = it,
                        localUserId = localUserId
                    )
                }?.show(
                    requireActivity().supportFragmentManager,
                    DetailsBottomSheet.TAG
                )
            }

            override fun actionReaction(reaction: String) {
                if (reaction.isNotEmpty()) {
                    msg.message.reaction = reaction
                    addReaction(msg.message)
                    chatAdapter.notifyItemChanged(msg.message.messagePosition)
                }
            }

            override fun addCustomReaction() {
                root.postDelayed({
                    openCustomEmojiKeyboard(msg.message)
                }, 200)
            }
        })
        bottomSheet.show(requireActivity().supportFragmentManager, ChatBottomSheet.TAG)
    }

    private fun handleMessageCopy(message: Message) {
        val clipboard =
            requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", message.body?.text.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            requireContext(),
            getString(R.string.text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openCustomEmojiKeyboard(message: Message) {
        setSendingAreaVisibility(View.GONE)

        lateinit var updatedEmojiPopup: EmojiPopup
        updatedEmojiPopup = EmojiPopup(
            rootView = bindingSetup.root,
            editText = bindingSetup.etMessage,
            onEmojiClickListener = { emoji ->
                message.reaction = emoji.unicode
                addReaction(message)
                setSendingAreaVisibility(View.VISIBLE)
                updatedEmojiPopup.dismiss()
                hideKeyboard(bindingSetup.root)
            },
            onEmojiPopupDismissListener = { setSendingAreaVisibility(View.VISIBLE) },
        )
        updatedEmojiPopup.toggle()
    }

    private fun setSendingAreaVisibility(visibility: Int) = with(bindingSetup) {
        if (View.VISIBLE == visibility) hideSendButton()
        etMessage.text?.clear()
        ivAdd.visibility = visibility
        clTyping.visibility = visibility
        ivCamera.visibility = visibility
    }

    private fun handleDownloadFile(message: MessageAndRecords) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            when {
                context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                } == PackageManager.PERMISSION_GRANTED -> {
                    Tools.downloadFile(requireContext(), message.message)
                }

                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    // TODO show why permission is needed
                }

                else -> {
                    storedMessage = message.message
                    storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else Tools.downloadFile(requireContext(), message.message)
    }

    private fun handleDownloadCancelFile(message: Message) {
        DialogError.getInstance(activity!!,
            getString(R.string.warning),
            getString(R.string.cancel_upload),
            getString(R.string.back),
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    // Ignore
                }

                override fun onSecondOptionClicked() {
                    viewModel.cancelUploadFile(messageId = message.localId.toString())
                    viewModel.deleteLocalMessage(message = message)
                    viewModel.updateUnreadCount(roomId = roomWithUsers!!.room.roomId)
                }
            })
    }

    private fun handleMediaNavigation(chatMessage: MessageAndRecords) {
        val mediaInfo: String = if (chatMessage.message.fromUserId == localUserId) {
            requireContext().getString(
                R.string.you_sent_on,
                Tools.fullDateFormat(chatMessage.message.createdAt!!)
            )
        } else {
            val userName =
                roomWithUsers!!.users.firstOrNull { it.id == chatMessage.message.fromUserId }!!.formattedDisplayName
            requireContext().getString(
                R.string.user_sent_on,
                userName,
                Tools.fullDateFormat(chatMessage.message.createdAt!!)
            )
        }

        val action =
            ChatMessagesFragmentDirections.actionChatMessagesFragmentToMediaFragment(
                mediaInfo = mediaInfo,
                message = chatMessage.message
            )

        findNavController().navigate(action, navOptionsBuilder)
    }

    private fun handleMessageReply(message: Message) = with(bindingSetup.replyAction) {
        replyId = message.id.toLong()

        val user = roomWithUsers!!.users.firstOrNull {
            it.id == message.fromUserId
        }
        tvUsername.text = user!!.formattedDisplayName

        when (message.type) {
            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                tvMessage.visibility = View.GONE
                ivReplyImage.visibility = View.VISIBLE
                if (Const.JsonFields.IMAGE_TYPE == message.type) {
                    tvReplyMedia.text = getString(
                        R.string.media,
                        context!!.getString(R.string.photo)
                    )
                    tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_camera_reply, 0, 0, 0
                    )
                }
                if (Const.JsonFields.VIDEO_TYPE == message.type) {
                    tvReplyMedia.text =
                        getString(R.string.media, context!!.getString(R.string.video))
                    tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_video_reply, 0, 0, 0
                    )
                }
                val mediaPath = Tools.getMediaFile(requireContext(), message)
                tvReplyMedia.visibility = View.VISIBLE
                Glide.with(this@ChatMessagesFragment)
                    .load(mediaPath)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.img_image_placeholder)
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivReplyImage)
            }

            Const.JsonFields.AUDIO_TYPE -> {
                tvMessage.visibility = View.GONE
                tvReplyMedia.visibility = View.VISIBLE
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.text =
                    getString(R.string.media, context!!.getString(R.string.audio))
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_audio_reply, 0, 0, 0
                )
            }

            Const.JsonFields.FILE_TYPE -> {
                tvMessage.visibility = View.GONE
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.visibility = View.VISIBLE
                tvReplyMedia.text =
                    getString(R.string.media, context!!.getString(R.string.file))
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_file_reply, 0, 0, 0
                )
            }

            else -> {
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.visibility = View.GONE
                tvMessage.visibility = View.VISIBLE
                val replyText = message.body?.text
                tvMessage.text = replyText
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }

    private fun handleMessageEdit(message: Message) = with(bindingSetup) {
        isEditing = true
        originalText = message.body?.text.toString()
        editedMessageId = message.id
        etMessage.setText(message.body?.text)
        ivAdd.rotation = ROTATION_ON

        etMessage.addTextChangedListener {
            if (isEditing) {
                if (!originalText.equals(it)) {
                    tvSave.visibility = View.VISIBLE
                    ivCamera.visibility = View.INVISIBLE
                } else {
                    tvSave.visibility = View.GONE
                    ivCamera.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addReaction(message: Message) {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Const.Networking.MESSAGE_ID, message.id)
        jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.REACTION)
        jsonObject.addProperty(Const.JsonFields.REACTION, message.reaction)
        viewModel.sendReaction(jsonObject)
    }

    private fun resetEditingFields() = with(bindingSetup) {
        editedMessageId = 0
        isEditing = false
        originalText = ""
        ivAdd.rotation = ROTATION_OFF
        tvSave.visibility = View.GONE
        ivCamera.visibility = View.VISIBLE
        etMessage.text!!.clear()
        etMessage.setText("")
    }

    private fun rotationAnimation() = with(bindingSetup) {
        vTransparent.visibility = View.GONE
        ivAdd.rotation = ROTATION_OFF
    }

    private fun showSendButton() = with(bindingSetup) {
        ivCamera.visibility = View.INVISIBLE
        ivButtonSend.visibility = View.VISIBLE
        clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = ivButtonSend.id
        }
        ivAdd.rotation = ROTATION_OFF
    }

    private fun hideSendButton() = with(bindingSetup) {
        ivCamera.visibility = View.VISIBLE
        ivButtonSend.visibility = View.GONE
        clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = ivCamera.id
        }
        ivAdd.rotation = ROTATION_OFF
    }

    private fun showDeleteMessageDialog(message: Message) {
        ChooserDialog.getInstance(requireContext(),
            getString(R.string.delete),
            null,
            getString(R.string.delete_for_everyone),
            getString(R.string.delete_for_me),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    deleteMessage(message, Const.UserActions.DELETE_MESSAGE_ALL)
                }

                override fun onSecondOptionClicked() {
                    deleteMessage(message, Const.UserActions.DELETE_MESSAGE_ME)
                }
            })
    }

    private fun deleteMessage(message: Message, target: String) {
        viewModel.deleteMessage(message.id, target)
        val deletedMessage =
            messagesRecords.firstOrNull { it.message.localId == message.localId }
        chatAdapter.notifyItemChanged(messagesRecords.indexOf(deletedMessage))
    }

    private fun editMessage() {
        if (editedMessageId != 0) {
            val jsonObject = JsonObject()
            jsonObject.addProperty(
                Const.JsonFields.TEXT_TYPE,
                bindingSetup.etMessage.text.toString().trim()
            )
            viewModel.editMessage(editedMessageId, jsonObject)
        }
    }

    private fun resendMessage(message: Message) {
        when (message.type) {
            Const.JsonFields.TEXT_TYPE -> {
                try {
                    sendMessage(
                        text = message.body?.text!!,
                        localId = message.localId.toString(),
                    )
                } catch (e: Exception) {
                    Timber.d("Send message exception: $e")
                }
            }

            Const.JsonFields.FILE_TYPE, Const.JsonFields.AUDIO_TYPE -> {
                val resendMessage = message.body?.file?.uri?.toUri()
                if (resendMessage != null) {
                    selectedFiles.add(resendMessage)
                    handleUserSelectedFile(selectedFiles)
                }
                viewModel.deleteLocalMessage(message)
            }

            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                if (message.originalUri != null) {
                    selectedFiles.add(message.originalUri!!.toUri())
                    handleUserSelectedFile(selectedFiles)
                    viewModel.deleteLocalMessage(message)
                } else {
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG)
                }
            }
        }
    }

    private fun sendMessage() {
        try {
            sendMessage(
                text = bindingSetup.etMessage.text.toString().trim(),
                localId = unsentMessages.first().localId!!,
            )
        } catch (e: Exception) {
            Timber.d("Send message exception: $e")
        }
    }

    private fun sendMessage(
        text: String,
        localId: String,
    ) {
        val jsonMessage = JsonMessage(
            text,
            Const.JsonFields.TEXT_TYPE,
            0,
            0,
            roomWithUsers!!.room.roomId,
            localId,
            replyId
        )

        val jsonObject = jsonMessage.messageToJson()
        Timber.d("Message object: $jsonObject")
        viewModel.sendMessage(jsonObject, localId)

        if (replyId != 0L) {
            replyId = 0L
        }
    }

    private fun createTempTextMessage() {
        val messageBody =
            MessageBody(null, bindingSetup.etMessage.text.toString().trim(), 1, 1, null, null)

        val tempMessage = Tools.createTemporaryMessage(
            getUniqueRandomId(unsentMessages),
            localUserId,
            roomWithUsers!!.room.roomId,
            Const.JsonFields.TEXT_TYPE,
            messageBody
        )

        Timber.d("Temporary message: $tempMessage")
        unsentMessages.add(0, tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    /** Files uploading */
    private fun handleUserSelectedFile(selectedFilesUris: MutableList<Uri>) {
        bindingSetup.ivCamera.visibility = View.GONE

        for (uri in selectedFilesUris) {
            val fileMimeType = getFileMimeType(context, uri)
            if ((fileMimeType?.contains(Const.JsonFields.IMAGE_TYPE) == true ||
                        fileMimeType?.contains(Const.JsonFields.VIDEO_TYPE) == true) &&
                !Tools.forbiddenMimeTypes(fileMimeType)
            ) {
                convertMedia(uri, fileMimeType)
            } else {
                filesSelected.add(uri)
                tempFilesToCreate.add(TempUri(uri, Const.JsonFields.FILE_TYPE))
            }
        }

        sendFile()
        startUploadService(uploadFiles)

        uploadFiles.clear()
        selectedFilesUris.clear()
        tempFilesToCreate.clear()
    }

    private fun convertMedia(uri: Uri, fileMimeType: String?) {
        val thumbnailUri: Uri
        val fileUri: Uri

        if (fileMimeType?.contains(Const.JsonFields.VIDEO_TYPE) == true) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)

            val duration =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            val bitRate =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLong() ?: 0

            if (Tools.getVideoSize(duration, bitRate)) {
                Toast.makeText(context, getString(R.string.video_error), Toast.LENGTH_LONG).show()
                return
            }

            val bitmap = mmr.frameAtTime
            val fileName = "VIDEO-${System.currentTimeMillis()}.mp4"
            val file =
                File(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)

            file.createNewFile()

            val filePath = file.absolutePath
            Tools.genVideoUsingMuxer(uri, filePath)
            fileUri = FileProvider.getUriForFile(
                MainApplication.appContext,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                file
            )
            val thumbnail =
                ThumbnailUtils.extractThumbnail(bitmap, bitmap!!.width, bitmap.height)
            thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail)
            tempFilesToCreate.add(TempUri(thumbnailUri, Const.JsonFields.VIDEO_TYPE))
            uriPairList.add(Pair(uri, thumbnailUri))

            mmr.release()
        } else {
            val bitmap =
                Tools.handleSamplingAndRotationBitmap(activity!!, uri, false)
            fileUri = Tools.convertBitmapToUri(activity!!, bitmap!!)

            val thumbnail =
                Tools.handleSamplingAndRotationBitmap(activity!!, fileUri, true)
            thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail!!)
            tempFilesToCreate.add(TempUri(thumbnailUri, Const.JsonFields.IMAGE_TYPE))
        }

        uriPairList.add(Pair(uri, thumbnailUri))
        thumbnailUris.add(thumbnailUri)
        currentMediaLocation.add(fileUri)
    }

    private fun sendFile() {
        if (tempFilesToCreate.isNotEmpty()) {
            for (tempFile in tempFilesToCreate) {
                createTempFileMessage(tempFile.uri, tempFile.type)
            }
            tempFilesToCreate.clear()

            if (unsentMessages.isNotEmpty()) {
                for (unsentMessage in unsentMessages) {
                    if (Const.JsonFields.IMAGE_TYPE == unsentMessage.type ||
                        Const.JsonFields.VIDEO_TYPE == unsentMessage.type
                    ) {
                        // Send thumbnail
                        uploadFiles(
                            isThumbnail = true,
                            uri = thumbnailUris.first(),
                            localId = unsentMessage.localId!!,
                            metadata = unsentMessage.body?.file?.metaData
                        )
                        // Send original image
                        uploadFiles(
                            isThumbnail = false,
                            uri = currentMediaLocation.first(),
                            localId = unsentMessage.localId,
                            metadata = unsentMessage.body?.file?.metaData
                        )
                        currentMediaLocation.removeFirst()
                        thumbnailUris.removeFirst()
                    } else if (filesSelected.isNotEmpty()) {
                        // Send file
                        uploadFiles(
                            isThumbnail = false,
                            uri = filesSelected.first(),
                            localId = unsentMessage.localId!!,
                            metadata = null
                        )
                        filesSelected.removeFirst()
                    }
                }
            }
        }
    }

    private fun createTempFileMessage(uri: Uri, type: String) {
        val tempMessage = FilesHelper.createTempFile(
            uri = uri,
            type = type,
            localUserId = localUserId,
            roomId = roomWithUsers!!.room.roomId,
            unsentMessages = unsentMessages
        )

        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
    }

    private fun uploadFiles(
        isThumbnail: Boolean,
        uri: Uri,
        localId: String,
        metadata: FileMetadata?,
    ) {
        val uploadData: MutableList<FileData> = ArrayList()
        uploadData.add(
            FilesHelper.uploadFile(
                isThumbnail,
                uri,
                localId,
                roomWithUsers!!.room.roomId,
                metadata
            )
        )
        uploadFiles.addAll(uploadData)
    }

    /** Upload service */
    private fun startUploadService(files: ArrayList<FileData>) {
        val intent = Intent(MainApplication.appContext, UploadService::class.java)
        intent.putParcelableArrayListExtra(Const.IntentExtras.FILES_EXTRA, files)
        MainApplication.appContext.startService(intent)
        activity?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as UploadService.UploadServiceBinder
            fileUploadService = binder.getService()
            fileUploadService.setCallbackListener(object : UploadService.FileUploadCallback {
                override fun updateUploadProgressBar(
                    progress: Int,
                    maxProgress: Int,
                    localId: String?
                ) {
                    val message = messagesRecords.firstOrNull { it.message.localId == localId }
                    message!!.message.uploadProgress = (progress * 100) / maxProgress

                    if (isVisible || isResumed) {
                        activity!!.runOnUiThread {
                            chatAdapter.notifyItemChanged(
                                messagesRecords.indexOf(
                                    message
                                )
                            )
                        }
                    }
                }

                override fun uploadingFinished(uploadedFiles: MutableList<FileData>) {
                    Tools.deleteTemporaryMedia(context!!)
                    context?.cacheDir?.deleteRecursively()

                    if (uploadedFiles.isNotEmpty()) {
                        uploadedFiles.forEach { item ->
                            if (item.messageStatus == Resource.Status.ERROR ||
                                item.messageStatus == Resource.Status.LOADING ||
                                item.messageStatus == null
                            ) {
                                if (!item.isThumbnail) {
                                    viewModel.updateMessages(
                                        messageStatus = Resource.Status.ERROR.toString(),
                                        localId = item.localId.toString()
                                    )
                                } else {
                                    val resendUri =
                                        uriPairList.find { it.second == item.fileUri }
                                    viewModel.updateLocalUri(
                                        localId = item.localId.toString(),
                                        uri = resendUri?.first.toString(),
                                    )
                                }
                            } else {
                                uriPairList.removeIf { it.second == item.fileUri }
                            }
                        }
                        uriPairList.clear()
                        uploadedFiles.clear()
                        unsentMessages.clear()
                        return
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("Service disconnected")
        }
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

    private fun onBackArrowPressed() {
        viewModel.updateUnreadCount(roomId = roomWithUsers!!.room.roomId)
        activity?.onBackPressedDispatcher?.onBackPressed()
        activity?.finish()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBlockedUsersList()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exoPlayer != null) {
            exoPlayer!!.release()
        }
        viewModel.unregisterSharedPrefsReceiver()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("List state store = ${bindingSetup.rvChat.layoutManager?.onSaveInstanceState()}")
        listState = bindingSetup.rvChat.layoutManager?.onSaveInstanceState()
    }
}
