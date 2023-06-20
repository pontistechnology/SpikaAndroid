package com.clover.studio.exampleapp.ui.main.chat

import android.Manifest
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.MainApplication
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.JsonMessage
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.MessageBody
import com.clover.studio.exampleapp.data.models.entity.MessageFile
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.FragmentChatMessagesBinding
import com.clover.studio.exampleapp.ui.ReactionContainer
import com.clover.studio.exampleapp.ui.ReactionsContainer
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.MessageSwipeController
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.clover.studio.exampleapp.utils.getChunkSize
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper.getFileMimeType
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonObject
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Runnable
import timber.log.Timber
import java.io.File

private const val SCROLL_DISTANCE_NEGATIVE = -300
private const val SCROLL_DISTANCE_POSITIVE = 300
private const val MIN_HEIGHT_DIFF = 150
private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f
private const val NEW_MESSAGE_ANIMATION_DURATION = 300L

enum class UploadMimeTypes {
    IMAGE, VIDEO, FILE, MEDIA
}

data class TempUri(
    val uri: Uri,
    val type: UploadMimeTypes,
)

@AndroidEntryPoint
class ChatMessagesFragment : BaseFragment(), ChatOnBackPressed {
    private val viewModel: ChatViewModel by activityViewModels()
    private val args: ChatMessagesFragmentArgs by navArgs()
    private lateinit var bindingSetup: FragmentChatMessagesBinding

    private lateinit var roomWithUsers: RoomWithUsers
    private var user: User? = null
    private var messagesRecords: MutableList<MessageAndRecords> = mutableListOf()
    private var unsentMessages: MutableList<Message> = ArrayList()
    private lateinit var storedMessage: Message
    private var localUserId: Int = 0

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var detailsMessageAdapter: MessageDetailsAdapter
    private lateinit var messageReactionAdapter: MessageReactionAdapter
    var itemTouchHelper: ItemTouchHelper? = null
    private var valueAnimator: ValueAnimator? = null

    private var currentMediaLocation: MutableList<Uri> = ArrayList()
    private var filesSelected: MutableList<Uri> = ArrayList()
    private var thumbnailUris: MutableList<Uri> = ArrayList()
    private var tempFilesToCreate: MutableList<TempUri> = ArrayList()
    private var photoImageUri: Uri? = null
    private var mediaType: UploadMimeTypes? = null
    private var uploadPieces = 0
    private var uploadInProgress = false
    private var isFetching = false
    private var directory: File? = null

    private var isAdmin = false
    private var progress = 0

    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetMessageActions: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetReplyAction: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetDetailsAction: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetReactionsAction: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheets: List<BottomSheetBehavior<ConstraintLayout>>

    private lateinit var storagePermission: ActivityResultLauncher<String>
    private var exoPlayer: ExoPlayer? = null

    private var avatarFileId = 0L
    private var userName = ""
    private var firstEnter = true
    private var isEditing = false
    private var originalText = ""
    private var editedMessageId = 0
    private var replyId: Long? = 0L
    private lateinit var emojiPopup: EmojiPopup

    private var scrollYDistance = 0
    private var heightDiff = 0
    private var newMessagesCount = 0

    private val chooseFileContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it != null) {
                for (uri in it) {
                    handleUserSelectedFile(uri)
                }
            }
        }

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it != null) {
                for (uri in it) {
                    handleUserSelectedFile(uri)
                }
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                if (photoImageUri != null) {
                    handleUserSelectedFile(photoImageUri!!)
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

        bottomSheets = listOf(
            bottomSheetReactionsAction,
            bottomSheetBehaviour,
            bottomSheetDetailsAction,
            bottomSheetMessageActions
        )

        roomWithUsers = (activity as ChatScreenActivity?)!!.roomWithUsers!!
        localUserId = viewModel.getLocalUserId()!!

        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            user = roomWithUsers.users.firstOrNull { it.id.toString() != localUserId.toString() }
        }

        emojiPopup = EmojiPopup(bindingSetup.root, bindingSetup.etMessage)

        // Timber.d("Load check: ChatMessagesFragment view created")
        // Check if we have left the room or if room is deleted, if so, disable bottom message interaction
        if (roomWithUsers.room.roomExit || roomWithUsers.room.deleted) {
            bindingSetup.clRoomExit.visibility = View.VISIBLE
            setUpAdapter()
            initializeObservers()
        } else {
            bindingSetup.clRoomExit.visibility = View.GONE
            checkStoragePermission()
            setUpAdapter()
            setUpMessageDetailsAdapter()
            setUpMessageReactionAdapter()
            initializeObservers()
            checkIsUserAdmin()
        }
        initViews()
        initListeners()

        // Clear notifications for this room
        NotificationManagerCompat.from(requireContext()).cancel(roomWithUsers.room.roomId)

        return bindingSetup.root
    }

    private fun initViews() {
        directory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            avatarFileId = user?.avatarFileId!!
            userName = user?.formattedDisplayName.toString()
        } else {
            avatarFileId = roomWithUsers.room.avatarFileId!!
            userName = roomWithUsers.room.name.toString()
        }

        setAvatarAndName(avatarFileId, userName)

        if (roomWithUsers.room.roomExit || roomWithUsers.room.deleted) {
            bindingSetup.chatHeader.ivVideoCall.setImageResource(R.drawable.img_video_call_disabled)
            bindingSetup.chatHeader.ivCallUser.setImageResource(R.drawable.img_call_user_disabled)
            bindingSetup.chatHeader.ivVideoCall.isEnabled = false
            bindingSetup.chatHeader.ivCallUser.isEnabled = false
        }

        bindingSetup.chatHeader.tvTitle.text = roomWithUsers.room.type
    }

    private fun setAvatarAndName(avatarFileId: Long, userName: String) {
        bindingSetup.chatHeader.tvChatName.text = userName
        Glide.with(this)
            .load(avatarFileId.let { Tools.getFilePathUrl(it) })
            .placeholder(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_user_placeholder
                )
            )
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(bindingSetup.chatHeader.ivUserImage)
    }

    private fun checkIsUserAdmin() {
        isAdmin = roomWithUsers.users.any { user ->
            user.id == localUserId && viewModel.isUserAdmin(roomWithUsers.room.roomId, user.id)
        }
    }

    private fun initListeners() {
        bindingSetup.chatHeader.clHeader.setOnClickListener {
            if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
                val bundle =
                    bundleOf(
                        Const.Navigation.USER_PROFILE to roomWithUsers.users.firstOrNull { user -> user.id != localUserId },
                        Const.Navigation.ROOM_ID to roomWithUsers.room.roomId,
                    )
                findNavController().navigate(
                    R.id.action_chatMessagesFragment_to_contactDetailsFragment2,
                    bundle
                )
            } else {
                val action =
                    ChatMessagesFragmentDirections.actionChatMessagesFragmentToChatDetailsFragment(
                        roomWithUsers,
                        isAdmin
                    )
                findNavController().navigate(action)
            }
        }

        bindingSetup.chatHeader.ivArrowBack.setOnClickListener {
            onBackArrowPressed()
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
            emojiPopup.toggle()
            emojiPopup.dismiss()
            emojiPopup.isShowing
            bindingSetup.ivAdd.rotation = ROTATION_OFF
        }

        bindingSetup.etMessage.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            }
        }

        // This listener is for keyboard opening
        bindingSetup.rvChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                if ((scrollYDistance <= 0) && (scrollYDistance > SCROLL_DISTANCE_NEGATIVE)
                    || (scrollYDistance >= 0) && (scrollYDistance < SCROLL_DISTANCE_POSITIVE)
                ) {
                    bindingSetup.rvChat.smoothScrollToPosition(0)
                }
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
                    bindingSetup.cvNewMessages.visibility = View.INVISIBLE
                    bindingSetup.cvBottomArrow.visibility = View.GONE
                    newMessagesCount = 0
                    scrollYDistance = 0
                }
            }
        })

        bindingSetup.root.viewTreeObserver.addOnGlobalLayoutListener {
            heightDiff = bindingSetup.root.rootView.height - bindingSetup.root.height
        }

        bindingSetup.cvNewMessages.setOnClickListener {
            bindingSetup.rvChat.scrollToPosition(0)
            bindingSetup.cvNewMessages.visibility = View.INVISIBLE
            scrollYDistance = 0
            newMessagesCount = 0
        }

        bindingSetup.cvBottomArrow.setOnClickListener {
            bindingSetup.rvChat.scrollToPosition(0)
            bindingSetup.cvBottomArrow.visibility = View.GONE
            scrollYDistance = 0
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
            bindingSetup.vHideTyping.visibility = View.GONE
            bindingSetup.vTransparent.visibility = View.GONE
            if (bindingSetup.etMessage.text?.isNotEmpty() == true) {
                createTempTextMessage()
                sendMessage()
            }
            bindingSetup.etMessage.setText("")
            hideSendButton()
        }

        bindingSetup.tvUnblock.setOnClickListener {
            DialogError.getInstance(requireContext(),
                getString(R.string.unblock_user),
                getString(R.string.unblock_description, bindingSetup.chatHeader.tvChatName.text),
                getString(R.string.no),
                getString(R.string.unblock),
                object : DialogInteraction {
                    override fun onSecondOptionClicked() {
                        roomWithUsers.users.firstOrNull { user -> user.id != localUserId }
                            ?.let { user -> viewModel.deleteBlockForSpecificUser(user.id) }
                    }
                })
        }

        bindingSetup.tvSave.setOnClickListener {
            editMessage()
            resetEditingFields()
        }

        initBottomSheetsListeners()

        /* Block dialog:
        bindingSetup.tvBlock.setOnClickListener {
            val userIdToBlock =
                roomWithUsers.users.firstOrNull { user -> user.id != localUserId }
            userIdToBlock?.let { idToBlock -> viewModel.blockUser(idToBlock.id) }
        }

        bindingSetup.tvOk.setOnClickListener {
            bindingSetup.clBlockContact.visibility = View.GONE
        }*/
    }

    private fun initBottomSheetsListeners() {
        // Initial visibility of bottom sheets
        bindingSetup.clBottomSheet.visibility = View.GONE
        bindingSetup.clBottomMessageActions.visibility = View.GONE
        bindingSetup.clDetailsAction.visibility = View.GONE
        bindingSetup.clBottomReplyAction.visibility = View.GONE
        bindingSetup.clReactionsDetails.visibility = View.GONE

        // Bottom sheet listeners
        bindingSetup.ivAdd.setOnClickListener {
            if (bottomSheetReplyAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                replyId = 0L
                bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
                bindingSetup.clBottomReplyAction.visibility = View.GONE
            }
            if (!isEditing) {
                if (bottomSheetBehaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
                    bindingSetup.ivAdd.rotation = ROTATION_ON
                    bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                    bindingSetup.vTransparent.visibility = View.VISIBLE
                    bindingSetup.clBottomSheet.visibility = View.VISIBLE
                }
            } else {
                resetEditingFields()
            }
        }

        bindingSetup.bottomSheet.btnFiles.setOnClickListener {
            chooseFile()
            rotationAnimation()
        }

        bindingSetup.messageActions.ivRemove.setOnClickListener {
            closeMessageSheet()
        }

        bindingSetup.reactionsDetails.ivRemove.setOnClickListener {
            bottomSheetReactionsAction.state = BottomSheetBehavior.STATE_COLLAPSED
            bindingSetup.clReactionsDetails.visibility = View.GONE
        }

        bindingSetup.replyAction.ivRemove.setOnClickListener {
            if (bottomSheetReplyAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
                bindingSetup.clBottomReplyAction.visibility = View.GONE
                replyId = 0L
            }
        }

        bindingSetup.detailsAction.ivRemove.setOnClickListener {
            if (bottomSheetDetailsAction.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_COLLAPSED
                bindingSetup.clDetailsAction.visibility = View.GONE
                bindingSetup.vTransparent.visibility = View.GONE
            }
        }

        bindingSetup.bottomSheet.ivRemove.setOnClickListener {
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            bindingSetup.clBottomSheet.visibility = View.GONE
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

        val bottomSheetBehaviorCallbackReactionDetails =
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Ignore
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        bindingSetup.vTransparent.visibility = View.GONE
                        val childNUmber = bindingSetup.reactionsDetails.llReactions.childCount
                        if (childNUmber != 0) {
                            bindingSetup.reactionsDetails.llReactions.removeViews(
                                1,
                                childNUmber - 1
                            )
                        }
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        bindingSetup.vTransparent.visibility = View.VISIBLE
                    }
                }
            }

        bottomSheetMessageActions.addBottomSheetCallback(bottomSheetBehaviorCallbackMessageAction)
        bottomSheetDetailsAction.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetBehaviour.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetReplyAction.addBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetReactionsAction.addBottomSheetCallback(bottomSheetBehaviorCallbackReactionDetails)
    }

    private fun closeMessageSheet() {
        bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
        bindingSetup.clBottomMessageActions.visibility = View.GONE
        bindingSetup.vTransparent.visibility = View.GONE
    }

    private fun initializeObservers() {
        viewModel.messageSendListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    // Delay next message sending by 2 seconds for better user experience.
                    // Could be removed if we deem it not needed.
                    if (!uploadInProgress) {
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            if (unsentMessages.isNotEmpty()) {
                                if (unsentMessages.first().type == Const.JsonFields.IMAGE_TYPE) {
                                    uploadImage()
                                } else if (unsentMessages.first().type == Const.JsonFields.VIDEO_TYPE) {
                                    uploadVideo()
                                } else if (filesSelected.isNotEmpty()) {
                                    uploadFile()
                                }
                            } else resetUploadFields()
                        }, 2000)
                    }
                    if (unsentMessages.isNotEmpty()){
                        val message = unsentMessages.find { msg ->  msg.localId == it.responseData?.data?.message?.localId}
                        unsentMessages.remove(message)
                    }
                }
                Resource.Status.ERROR -> {
                    Timber.d("Message send fail")
                }
                else -> Timber.d("Other error")
            }
            senderScroll()
        })

        viewModel.getMessageAndRecords(roomWithUsers.room.roomId).observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    messagesRecords.clear()
                    if (it.responseData?.isNotEmpty() == true) {
                        /* Block dialog:
                        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
                            val containsElement =
                                it.responseData.message.any { message -> localUserId == message.message.fromUserId }
                            if (containsElement) bindingSetup.clBlockContact.visibility =
                                View.GONE
                            else bindingSetup.clBlockContact.visibility = View.VISIBLE
                        } */

                        it.responseData.forEach { msg ->
                            messagesRecords.add(msg)
                        }

                        chatAdapter.submitList(messagesRecords.toList())
                        updateSwipeController()

                        if (firstEnter) {
                            newMessagesCount = 0
                            bindingSetup.rvChat.scrollToPosition(0)
                            firstEnter = false
                        }
                        // This else clause handles the issue where the firs message in the chat failed
                        // to be sent or uploaded. It would remain in the list otherwise.
                    } else chatAdapter.submitList(messagesRecords.toList())
                }

                Resource.Status.LOADING -> {
                    // Loading
                }

                else -> {
                    // Error
                }
            }
        }

        viewModel.newMessageReceivedListener.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (message.roomId == roomWithUsers.room.roomId) {
                    // Notify backend of messages seen
                    viewModel.sendMessagesSeen(roomWithUsers.room.roomId)
                    newMessagesCount++
                    showNewMessage()
                }
            }
        }

        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
                if (it?.isNotEmpty() == true) {
                    viewModel.fetchBlockedUsersLocally(it)
                } // else bindingSetup.clContactBlocked.visibility = View.GONE
            }

            viewModel.blockedListListener.observe(viewLifecycleOwner, EventObserver {
                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        Timber.d("Blocked users fetched successfully")
//                        /*  Block dialog:
                        if (it.responseData != null) {
                            val containsElement =
                                roomWithUsers.users.any { user -> it.responseData.find { blockedUser -> blockedUser.id == user.id } != null }
                            if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
                                if (containsElement) {
                                    bindingSetup.clContactBlocked.visibility = View.VISIBLE
//                                    bindingSetup.clBlockContact.visibility = View.GONE
                                } else bindingSetup.clContactBlocked.visibility = View.GONE
                            }
                        } else bindingSetup.clContactBlocked.visibility = View.GONE
//                        */
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
                                        // ignore
                                    }
                                })
                        }
                    }

                    else -> Timber.d("Other error")
                }
            })
        }

        viewModel.fileUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.LOADING -> {
                    try {
                        if (progress <= uploadPieces) {
                            updateUploadProgressBar(
                                progress + 1,
                                uploadPieces,
                                unsentMessages.first().localId!!
                            )
                            progress++
                        } else progress = 0
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on piece")
                        handleUploadError(UploadMimeTypes.FILE, it.message)
                    }
                }

                Resource.Status.SUCCESS -> {
                    try {
                        requireActivity().runOnUiThread {
                            Timber.d("Successfully sent file")
                            if (it.responseData?.fileId!! > 0) it.responseData.messageBody?.fileId =
                                it.responseData.fileId
                            sendMessage(
                                it.responseData.fileType,
                                it.responseData.messageBody?.text!!,
                                it.responseData.messageBody.fileId!!,
                                0,
                                unsentMessages.first().localId!!,
                            )
                            filesSelected.removeFirst()
                            uploadInProgress = false
                        }
                        // update room data
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on verify")
                        handleUploadError(UploadMimeTypes.FILE, it.message)
                    }
                }

                Resource.Status.ERROR -> {
                    handleUploadError(UploadMimeTypes.FILE, it.message)
                }

                else -> Timber.d("Other upload error")
            }
        })

        viewModel.mediaUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.LOADING -> {
                    try {
                        if (it.message == "false") {
                            if (progress <= uploadPieces) {
                                updateUploadProgressBar(
                                    progress + 1,
                                    uploadPieces,
                                    unsentMessages.first().localId!!
                                )
                                progress++
                            } else progress = 0
                        }
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on piece")
                        handleUploadError(UploadMimeTypes.MEDIA, it.message)
                    }
                }

                Resource.Status.SUCCESS -> {
                    try {
                        if (!it.responseData?.isThumbnail!!) {
                            if (it.responseData.fileId > 0) it.responseData.messageBody?.fileId =
                                it.responseData.fileId

                            sendMessage(
                                it.responseData.fileType,
                                it.responseData.messageBody?.text!!,
                                it.responseData.messageBody.fileId!!,
                                it.responseData.messageBody.thumbId!!,
                                unsentMessages.first().localId!!,
                            )
                            currentMediaLocation.removeFirst()
                            uploadInProgress = false
                        } else {
                            if (it.responseData.thumbId > 0) it.responseData.messageBody?.thumbId =
                                it.responseData.thumbId
                            if (it.responseData.mimeType.contains(Const.JsonFields.IMAGE_TYPE)) {
                                it.responseData.messageBody?.let { messageBody ->
                                    uploadMedia(
                                        false,
                                        currentMediaLocation.first(),
                                        messageBody
                                    )
                                }
                            } else {
                                it.responseData.messageBody?.let { messageBody ->
                                    uploadMedia(
                                        false,
                                        currentMediaLocation.first(),
                                        messageBody
                                    )
                                }
                            }
                            thumbnailUris.removeFirst()
                        }
                    } catch (ex: Exception) {
                        Timber.d("File upload failed on verified")
                        handleUploadError(UploadMimeTypes.MEDIA, it.message)
                    }
                }

                Resource.Status.ERROR -> {
                    handleUploadError(UploadMimeTypes.MEDIA, it.message)
                }

                else -> Timber.d("Other upload error")
            }
        })

        viewModel.roomInfoUpdated.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData?.data?.room?.roomId == roomWithUsers.room.roomId) {
                        val avatarFile = it.responseData.data.room.avatarFileId ?: 0L
                        val roomName = it.responseData.data.room.name ?: ""
                        roomWithUsers.room.apply {
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
    }

    private fun senderScroll(){
        if ((scrollYDistance <= 0) && (scrollYDistance > SCROLL_DISTANCE_NEGATIVE)
            || (scrollYDistance >= 0) && (scrollYDistance < SCROLL_DISTANCE_POSITIVE)
        ) {
            scrollToPosition()
            bindingSetup.cvBottomArrow.visibility = View.GONE
        } else {
            bindingSetup.cvBottomArrow.visibility = View.VISIBLE
        }
    }

    private fun showNewMessage() {
        valueAnimator?.end()
        valueAnimator?.removeAllUpdateListeners()
        bindingSetup.cvBottomArrow.visibility = View.GONE

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
        } else {
            // If we are somewhere up in chat, show new message dialog
            bindingSetup.cvNewMessages.visibility = View.VISIBLE

            if (newMessagesCount == 1) {
                bindingSetup.tvNewMessage.text =
                    getString(R.string.new_messages, newMessagesCount.toString(), "").trim()

                val startWidth = bindingSetup.ivBottomArrow.width
                val endWidth = (bindingSetup.tvNewMessage.width + bindingSetup.ivBottomArrow.width)

                valueAnimator = ValueAnimator.ofInt(startWidth, endWidth).apply {
                    addUpdateListener { valueAnimator ->
                        val layoutParams = bindingSetup.cvNewMessages.layoutParams
                        layoutParams.width = valueAnimator.animatedValue as Int
                        bindingSetup.cvNewMessages.layoutParams = layoutParams
                    }
                    duration = NEW_MESSAGE_ANIMATION_DURATION
                }
                valueAnimator?.start()

            } else {
                bindingSetup.tvNewMessage.text =
                    getString(R.string.new_messages, newMessagesCount.toString(), "s").trim()
            }
        }
        return
    }

    private fun showBottomArrow() {
        if (newMessagesCount == 0) {
            if (heightDiff >= MIN_HEIGHT_DIFF && scrollYDistance > SCROLL_DISTANCE_POSITIVE) {
                scrollYDistance -= heightDiff
            }
            if (!((scrollYDistance <= 0) && (scrollYDistance > SCROLL_DISTANCE_NEGATIVE)
                        || (scrollYDistance >= 0) && (scrollYDistance < SCROLL_DISTANCE_POSITIVE))
            ) {
                bindingSetup.cvNewMessages.visibility = View.INVISIBLE
                bindingSetup.cvBottomArrow.visibility = View.VISIBLE
            } else {
                bindingSetup.cvBottomArrow.visibility = View.GONE
            }
        }
    }

    private fun scrollToPosition() {
        newMessagesCount = 0
        bindingSetup.rvChat.smoothScrollToPosition(0)
        scrollYDistance = 0
    }

    private fun setUpAdapter() {
        exoPlayer = ExoPlayer.Builder(this.context!!).build()
        chatAdapter = ChatAdapter(
            context!!,
            localUserId,
            roomWithUsers.users,
            exoPlayer!!,
            roomWithUsers.room.type,
            onMessageInteraction = { event, message ->
                // Block dialog:
                // if (bindingSetup.clContactBlocked.visibility != View.VISIBLE) {
                if (!roomWithUsers.room.deleted && !roomWithUsers.room.roomExit) {
                    run {
                        when (event) {
                            Const.UserActions.DOWNLOAD_FILE -> handleDownloadFile(message)
                            Const.UserActions.DOWNLOAD_CANCEL -> handleDownloadCancelFile()
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
                // }
            }
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        bindingSetup.rvChat.itemAnimator = null
        bindingSetup.rvChat.adapter = chatAdapter
        layoutManager.stackFromEnd = true
        bindingSetup.rvChat.layoutManager = layoutManager

        bindingSetup.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                if (lastVisiblePosition == totalItemCount - 1 && !isFetching) {
                    Timber.d("Fetching next batch of data")
                    viewModel.fetchNextSet(roomWithUsers.room.roomId)
                    isFetching = true
                } else if (lastVisiblePosition != totalItemCount - 1) {
                    isFetching = false // Reset the flag when user scrolls away from the bottom
                }
                showBottomArrow()
            }
        })

        // Notify backend of messages seen
        viewModel.sendMessagesSeen(roomWithUsers.room.roomId)

        // Update room visited
        viewModel.updateUnreadCount(roomId = roomWithUsers.room.roomId)
    }

    private fun updateSwipeController() {
        itemTouchHelper?.attachToRecyclerView(null)
        val messageSwipeController =
            MessageSwipeController(context!!, messagesRecords, onSwipeAction = { action, position ->
                when (action) {
                    Const.UserActions.ACTION_RIGHT -> {
                        bottomSheetReplyAction.state = BottomSheetBehavior.STATE_EXPANDED
                        bindingSetup.clBottomReplyAction.visibility = View.VISIBLE
                        handleMessageReply(messagesRecords[position].message)
                    }

                    Const.UserActions.ACTION_LEFT -> {
                        bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_EXPANDED
                        bindingSetup.clDetailsAction.visibility = View.VISIBLE
                        getDetailsList(messagesRecords[position].message)
                    }
                }
            })

        itemTouchHelper = ItemTouchHelper(messageSwipeController)
        itemTouchHelper!!.attachToRecyclerView(bindingSetup.rvChat)
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

    private fun handleShowReactions(messageRecords: MessageAndRecords) {
        bindingSetup.vTransparent.visibility = View.VISIBLE
        bottomSheetReactionsAction.state = BottomSheetBehavior.STATE_EXPANDED
        bindingSetup.clReactionsDetails.visibility = View.VISIBLE
        bindingSetup.reactionsDetails.tvAllReactions.setBackgroundResource(R.drawable.bg_reaction_selected)

        val reactionsList = messageRecords.records!!.filter { it.reaction != null }
            .sortedByDescending { it.createdAt }
        // Default view - all reactions:
        messageReactionAdapter.submitList(reactionsList)

        // Group same reactions
        val reactionList = reactionsList.groupBy { it.reaction }.mapValues { it.value.size }

        // Add reaction views:
        if (reactionList.isNotEmpty()) {
            for (reaction in reactionList) {
                val reactionView = ReactionContainer(
                    requireActivity(),
                    null,
                    reaction.key.toString(),
                    reaction.value.toString()
                )
                bindingSetup.reactionsDetails.llReactions.addView(reactionView)
            }
        }

        // Set listeners to reaction views and submit new, filtered list of reactions to adapter
        var currentlySelectedTextView: View? = null

        for (child in bindingSetup.reactionsDetails.llReactions.children) {
            child.setOnClickListener { view ->
                // Remove / add backgrounds for views
                if (view != currentlySelectedTextView) {
                    currentlySelectedTextView?.background = null
                    view.setBackgroundResource(R.drawable.bg_reaction_selected)
                    currentlySelectedTextView = view
                }

                val childIndex = bindingSetup.reactionsDetails.llReactions.indexOfChild(view)
                if (childIndex == 0) {
                    messageReactionAdapter.submitList(reactionsList)
                } else {
                    bindingSetup.reactionsDetails.tvAllReactions.background = null
                    val reactionView: ReactionContainer =
                        bindingSetup.reactionsDetails.llReactions.getChildAt(childIndex) as ReactionContainer
                    val reactionText = reactionView.showReaction()
                    messageReactionAdapter.submitList(reactionsList.filter { it.reaction == reactionText })
                }
            }
        }
    }

    private fun handleMessageReplyClick(msg: MessageAndRecords) {
        val position =
            messagesRecords.indexOfFirst { it.message.createdAt == msg.message.body?.referenceMessage?.createdAt }
        if (position != -1) {
            bindingSetup.rvChat.scrollToPosition(position)
        }
    }

    private fun handleMessageResend(message: MessageAndRecords){
        ChooserDialog.getInstance(requireContext(),
            "Resend",
            null,
            "Resend message",
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

    private fun handleMessageAction(msg: MessageAndRecords) {
        if (msg.message.deleted == null || msg.message.deleted == true) {
            return
        }

        val reactionsContainer = ReactionsContainer(this.context!!, null)
        bindingSetup.messageActions.reactionsContainer.addView(reactionsContainer)
        bottomSheetMessageActions.state = BottomSheetBehavior.STATE_EXPANDED
        bindingSetup.clBottomMessageActions.visibility = View.VISIBLE
        bindingSetup.vTransparent.visibility = View.VISIBLE

        val localId = viewModel.getLocalUserId()
        val fromUserId = msg.message.fromUserId

        bindingSetup.messageActions.tvDelete.visibility =
            if (fromUserId == localId) View.VISIBLE else View.GONE

        bindingSetup.messageActions.tvEdit.visibility =
            if (fromUserId == localId && Const.JsonFields.TEXT_TYPE == msg.message.type) View.VISIBLE else View.GONE

        bindingSetup.messageActions.tvCopy.visibility =
            if (Const.JsonFields.TEXT_TYPE == msg.message.type) View.VISIBLE else View.GONE

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
            bindingSetup.clBottomReplyAction.visibility = View.VISIBLE
            handleMessageReply(msg.message)
        }

        bindingSetup.messageActions.tvDetails.setOnClickListener {
            bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
            bindingSetup.clBottomMessageActions.visibility = View.GONE
            getDetailsList(msg.message)
            bottomSheetDetailsAction.state = BottomSheetBehavior.STATE_EXPANDED
            bindingSetup.clDetailsAction.visibility = View.VISIBLE
        }

        bindingSetup.messageActions.tvCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("", msg.message.body?.text.toString())
            clipboard.setPrimaryClip(clip)
            bottomSheetMessageActions.state = BottomSheetBehavior.STATE_COLLAPSED
            bindingSetup.clBottomMessageActions.visibility = View.GONE
            Toast.makeText(requireContext(), getString(R.string.text_copied), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleDownloadFile(message: MessageAndRecords) {
        // Seems that WRITE_EXTERNAL_STORAGE check returns true for Android versions 12 and below,
        // but false for Android version 13. If scoped storage is correctly implemented, the
        // download will work without the need to check permission.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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
        } else Tools.downloadFile(context!!, message.message)
    }

    private fun handleDownloadCancelFile() {
        showUploadError(getString(R.string.upload_file_in_progress), false)
    }

    private fun handleMediaNavigation(chatMessage: MessageAndRecords) {
        val mediaInfo: String = if (chatMessage.message.fromUserId == localUserId) {
            context!!.getString(
                R.string.you_sent_on,
                Tools.fullDateFormat(chatMessage.message.createdAt!!)
            )
        } else {
            val userName =
                roomWithUsers.users.firstOrNull { it.id == chatMessage.message.fromUserId }!!.formattedDisplayName
            context!!.getString(
                R.string.user_sent_on,
                userName,
                Tools.fullDateFormat(chatMessage.message.createdAt!!)
            )
        }

        val action =
            ChatMessagesFragmentDirections.actionChatMessagesFragment2ToVideoFragment2(
                mediaInfo = mediaInfo,
                message = chatMessage.message
            )
        findNavController().navigate(action)
    }

    private fun handleMessageReply(message: Message) {
        bindingSetup.vTransparent.visibility = View.VISIBLE
        replyId = message.id.toLong()
        val backgroundResId = if (message.fromUserId == localUserId) {
            R.drawable.bg_message_send
        } else {
            R.drawable.bg_message_received
        }
        bindingSetup.replyAction.clReplyContainer.setBackgroundResource(backgroundResId)

        val user = roomWithUsers.users.firstOrNull {
            it.id == message.fromUserId
        }
        bindingSetup.replyAction.tvUsername.text = user!!.formattedDisplayName

        when (message.type) {
            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                bindingSetup.replyAction.tvMessage.visibility = View.GONE
                bindingSetup.replyAction.ivReplyImage.visibility = View.VISIBLE
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
                val mediaPath = Tools.getMediaFile(this.context!!, message)
                bindingSetup.replyAction.tvReplyMedia.visibility = View.VISIBLE
                Glide.with(this)
                    .load(mediaPath)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.img_image_placeholder)
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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

    private fun getDetailsList(detailsMessage: Message) {
        val senderId = detailsMessage.fromUserId

        /* Adding a message record for the sender so that it can be sent to the adapter */
        val senderMessageRecord = MessageRecords(
            id = 0,
            messageId = detailsMessage.id,
            userId = detailsMessage.fromUserId!!,
            type = Const.JsonFields.SENT,
            reaction = null,
            modifiedAt = detailsMessage.modifiedAt,
            createdAt = detailsMessage.createdAt!!,
            null
        )

        /* In the messageDetails list, we save message records for a specific message,
         remove reactions from those records(because we only need the seen and delivered types),
         remove the sender from the seen/delivered list and sort the list so that first we see
         seen and then delivered. */
        val messageDetails =
            messagesRecords.filter { it.message.id == detailsMessage.id }
                .flatMap { it.records!! }
                .filter { Const.JsonFields.REACTION != it.type }
                .filter { it.userId != detailsMessage.fromUserId }
                .sortedByDescending { it.type }
                .toMutableList()

        /* Then we add the sender of the message to the first position of the messageDetails list
        * so that we can display it in the RecyclerView */
        messageDetails.add(0, senderMessageRecord)

        /* If the room type is a group and the current user is not the sender, remove it from the list.*/
        if ((Const.JsonFields.GROUP == roomWithUsers.room.type) && (senderId != localUserId)) {
            val filteredMessageDetails =
                messageDetails.filter { it.userId != localUserId }.toMutableList()
            detailsMessageAdapter.submitList(ArrayList(filteredMessageDetails))
        } else {
            detailsMessageAdapter.submitList(ArrayList(messageDetails))
        }
    }

    private fun addReaction(message: Message) {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Const.Networking.MESSAGE_ID, message.id)
        jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.REACTION)
        jsonObject.addProperty(Const.JsonFields.REACTION, message.reaction)
        viewModel.sendReaction(jsonObject)
    }

    private fun resetEditingFields() {
        editedMessageId = 0
        isEditing = false
        originalText = ""
        bindingSetup.ivAdd.rotation = ROTATION_OFF
        bindingSetup.tvSave.visibility = View.GONE
        bindingSetup.ivCamera.visibility = View.VISIBLE
        bindingSetup.ivMicrophone.visibility = View.VISIBLE
        bindingSetup.etMessage.text!!.clear()
        bindingSetup.etMessage.setText("")
    }

    private fun rotationAnimation() {
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        bindingSetup.clBottomSheet.visibility = View.GONE
        bindingSetup.vTransparent.visibility = View.GONE
        bindingSetup.ivAdd.rotation = ROTATION_OFF
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

    private fun hideSendButton() {
        bindingSetup.ivCamera.visibility = View.VISIBLE
        bindingSetup.ivMicrophone.visibility = View.VISIBLE
        bindingSetup.ivButtonSend.visibility = View.GONE
        bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = bindingSetup.ivCamera.id
        }
        bindingSetup.ivAdd.rotation = ROTATION_OFF
        bottomSheetReplyAction.state = BottomSheetBehavior.STATE_COLLAPSED
        bindingSetup.clBottomReplyAction.visibility = View.GONE
    }

    private fun showDeleteMessageDialog(message: Message) {
        ChooserDialog.getInstance(requireContext(),
            getString(R.string.delete),
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

    private fun resendMessage(message: Message){
        try {
            sendMessage (
                messageFileType = Const.JsonFields.TEXT_TYPE,
                text = message.body?.text!!,
                fileId = 0,
                thumbId = 0,
                message.localId.toString(),
            )
        } catch (e: Exception) {
            Timber.d("Send message exception: $e")
        }
    }

    private fun sendMessage() {
        try {
            sendMessage(
                messageFileType = Const.JsonFields.TEXT_TYPE,
                text = bindingSetup.etMessage.text.toString(),
                0,
                0,
                unsentMessages.first().localId!!,
            )
        } catch (e: Exception) {
            Timber.d("Send message exception: $e")
        }
    }

    private fun sendMessage(
        messageFileType: String,
        text: String,
        fileId: Long,
        thumbId: Long,
        localId: String,
    ) {
        val jsonMessage = JsonMessage(
            text,
            messageFileType,
            fileId,
            thumbId,
            roomWithUsers.room.roomId,
            localId,
            replyId
        )

        val jsonObject = jsonMessage.messageToJson()
        Timber.d("Message object: $jsonObject")
        viewModel.sendMessage(jsonObject)

        if (replyId != 0L) {
            replyId = 0L
        }
    }

    private fun createTempTextMessage() {
        val messageBody =
            MessageBody(null, bindingSetup.etMessage.text.toString(), 1, 1, null, null)

        val tempMessage = Tools.createTemporaryMessage(
            getUniqueRandomId(),
            localUserId,
            roomWithUsers.room.roomId,
            Const.JsonFields.TEXT_TYPE,
            messageBody
        )

        Timber.d("Temporary message: $tempMessage")
        unsentMessages.add(0, tempMessage)
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

        val messageBody = MessageBody(
            null,
            null,
            1,
            1,
            MessageFile(1, fileName, "", fileStream.length(), null, null),
            null
        )

        var type = activity!!.contentResolver.getType(uri)
        type = if (type == Const.FileExtensions.AUDIO) {
            Const.JsonFields.AUDIO_TYPE
        } else {
            Const.JsonFields.FILE_TYPE
        }

        val tempMessage = Tools.createTemporaryMessage(
            getUniqueRandomId(),
            localUserId,
            roomWithUsers.room.roomId,
            type,
            messageBody
        )
        unsentMessages.add(tempMessage)

        viewModel.storeMessageLocally(tempMessage)

        inputStream.close()
    }

    /**
     * Creating temporary media message which will be shown to the user inside of the
     * chat adapter while the media file is uploading
     *
     * @param mediaUri uri of the media file for which a temporary image file will be created
     * which will hold the bitmap thumbnail.
     */
    private fun createTempMediaMessage(mediaUri: Uri) {
        val messageBody = MessageBody(
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
            null
        )

        // Media file is always thumbnail first. Therefore, we are sending CHAT_IMAGE as type
        val tempMessage = Tools.createTemporaryMessage(
            getUniqueRandomId(),
            localUserId,
            roomWithUsers.room.roomId,
            Const.JsonFields.IMAGE_TYPE,
            messageBody
        )

        val imagePath =
            Tools.saveMediaToStorage(
                context!!,
                requireActivity().contentResolver,
                mediaUri,
                tempMessage.localId
            )
        Timber.d("ImagePath: $imagePath")
        Timber.d("Local id: ${tempMessage.localId}")
        Timber.d("Temporary message: $tempMessage")

        unsentMessages.add(tempMessage)
        viewModel.storeMessageLocally(tempMessage)
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

    private fun uploadThumbnail() {
        mediaType = UploadMimeTypes.IMAGE
        uploadMedia(true, thumbnailUris.first(), messageBody = null)
    }

    private fun uploadVideoThumbnail() {
        mediaType = UploadMimeTypes.VIDEO
        uploadMedia(true, thumbnailUris.first(), messageBody = null)
    }

    private fun uploadImage() {
        uploadThumbnail()
    }

    private fun uploadVideo() {
        uploadVideoThumbnail()
    }

    /**
     * Method used for uploading files to the backend
     */
    private fun uploadFile() {
        uploadInProgress = true
        val messageBody = MessageBody(null, "", 0, 0, null, null)
        val inputStream =
            activity!!.contentResolver.openInputStream(filesSelected.first())

        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        val cr = activity!!.contentResolver
        cr.query(filesSelected.first(), projection, null, null, null)?.use { metaCursor ->
            if (metaCursor.moveToFirst()) {
                fileName = metaCursor.getString(0)
            }
        }

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            getFileMimeType(context!!, filesSelected.first())!!,
            fileName
        )

        uploadPieces =
            if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
            else (fileStream.length() / getChunkSize(fileStream.length())).toInt()
        progress = 0

        var type = getFileMimeType(context!!, filesSelected.first())!!
        type = if (Const.FileExtensions.AUDIO == type) {
            Const.JsonFields.AUDIO_TYPE
        } else {
            Const.JsonFields.FILE_TYPE
        }

        viewModel.startUploadFile()

        viewModel.uploadFile(
            requireActivity(),
            filesSelected.first(),
            uploadPieces,
            fileStream,
            type,
            messageBody
        )

        inputStream.close()
    }

    /**
     * One method used for uploading images and video files. They can be discerned by the
     * mediaType field send to the constructor.
     *
     * @param isThumbnail Declare if a thumbnail is being sent or a media file
     * @param uri Uri of the media/thumbnail file
     */
    private fun uploadMedia(
        isThumbnail: Boolean,
        uri: Uri,
        messageBody: MessageBody?
    ) {
        uploadInProgress = true
        var messageBodyNew = messageBody
        if (messageBodyNew == null) {
            messageBodyNew = MessageBody(null, "", 0, 0, null, null)
        }
        val inputStream =
            activity!!.contentResolver.openInputStream(uri)

        val fileStream = Tools.copyStreamToFile(
            activity!!,
            inputStream!!,
            getFileMimeType(context!!, uri)!!
        )
        uploadPieces =
            if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
            else (fileStream.length() / getChunkSize(fileStream.length())).toInt()
        progress = 0

        val fileType: String =
            if (getFileMimeType(context!!, uri)!!.contains(Const.JsonFields.IMAGE_TYPE)) {
                Const.JsonFields.IMAGE_TYPE
            } else {
                if (isThumbnail) {
                    Const.JsonFields.IMAGE_TYPE
                } else {
                    Const.JsonFields.VIDEO_TYPE
                }
            }

        viewModel.startUploadFile()

        viewModel.uploadMedia(
            requireActivity(),
            uri,
            fileType,
            uploadPieces,
            fileStream,
            messageBodyNew,
            isThumbnail
        )

        inputStream.close()
    }

    /**
     * Reset upload fields and clear local cache on success or critical fail
     */
    private fun resetUploadFields() {
        Timber.d("Resetting upload")
        viewModel.deleteLocalMessages(unsentMessages)

        currentMediaLocation.clear()
        filesSelected.clear()
        thumbnailUris.clear()

        Tools.deleteTemporaryMedia(context!!)

        uploadInProgress = false
        //unsentMessages.clear()
        context?.cacheDir?.deleteRecursively()
    }

    /**
     * Method handles error for specific files and checks if it should continue uploading other
     * files waiting in row, if there are any.
     *
     * Also displays a toast message for failed uploads.
     */
    private fun handleUploadError(typeFailed: UploadMimeTypes, message: String?) {
        if (UploadMimeTypes.MEDIA == typeFailed) {
            if (currentMediaLocation.isNotEmpty()) {
                currentMediaLocation.removeFirst()
            }
            if (unsentMessages.isNotEmpty()) {
                viewModel.deleteLocalMessage(unsentMessages.first())
            }
            unsentMessages.removeFirst()

            if (currentMediaLocation.isNotEmpty()) {
                if (getFileMimeType(
                        context!!,
                        currentMediaLocation.first()
                    )?.contains(Const.JsonFields.IMAGE_TYPE) == true
                ) {
                    uploadImage()
                } else {
                    uploadVideo()
                }
            } else if (filesSelected.isNotEmpty()) {
                uploadFile()
            } else resetUploadFields()

        } else if (UploadMimeTypes.FILE == typeFailed) {
            filesSelected.removeFirst()
            viewModel.deleteLocalMessage(unsentMessages.first())
            unsentMessages.removeFirst()

            if (filesSelected.isNotEmpty()) {
                uploadFile()
            } else resetUploadFields()
        } else resetUploadFields()

        val description = if (message == getString(R.string.canceled_file_upload)) {
            getString(R.string.canceled_file_upload)
        } else {
            getString(R.string.failed_file_upload)
        }

        Toast.makeText(
            activity!!.baseContext,
            description,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showUploadError(errorMessage: String, exit: Boolean) {
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
                    viewModel.updateUnreadCount(roomId = roomWithUsers.room.roomId)
                    if (unsentMessages.isNotEmpty()) {
                        viewModel.deleteLocalMessages(unsentMessages)
                    }

                    viewModel.cancelUploadFile()
                    uploadInProgress = false

                    if (exit) {
                        activity!!.finish()
                    }
                }
            })
    }

    private fun handleUserSelectedFile(uri: Uri) {
        bindingSetup.ivCamera.visibility = View.GONE

        val fileMimeType = getFileMimeType(context, uri)
        if (fileMimeType?.contains(Const.JsonFields.VIDEO_TYPE) == true && !fileMimeType.contains(
                Const.JsonFields.AVI_TYPE
            )
        ) {
            convertVideo(uri)
        } else if (fileMimeType?.contains(Const.JsonFields.IMAGE_TYPE) == true && !fileMimeType.contains(
                Const.JsonFields.SVG_TYPE
            )
        ) {
            convertImageToBitmap(uri)
        } else {
            filesSelected.add(uri)
            tempFilesToCreate.add(TempUri(uri, UploadMimeTypes.FILE))
        }
        sendFile()
    }

    private fun convertVideo(videoUri: Uri) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, videoUri)
        val bitmap = mmr.frameAtTime

        // This will actually stop the UI block while decoding the video
        val fileName = "VIDEO-${System.currentTimeMillis()}.mp4"
        val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)

        file.createNewFile()

        val filePath = file.absolutePath
        Tools.genVideoUsingMuxer(videoUri, filePath)
        val fileUri = FileProvider.getUriForFile(
            MainApplication.appContext,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )
        val thumbnail =
            ThumbnailUtils.extractThumbnail(bitmap, bitmap!!.width, bitmap.height)
        val thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail)

        thumbnailUris.add(thumbnailUri)
        currentMediaLocation.add(fileUri)
        tempFilesToCreate.add(TempUri(thumbnailUri, UploadMimeTypes.MEDIA))
    }

    private fun convertImageToBitmap(imageUri: Uri?) {
        val bitmap =
            Tools.handleSamplingAndRotationBitmap(activity!!, imageUri, false)
        val bitmapUri = Tools.convertBitmapToUri(activity!!, bitmap!!)

        activity!!.runOnUiThread { showSendButton() }

        val thumbnail =
            Tools.handleSamplingAndRotationBitmap(activity!!, bitmapUri, true)
        val thumbnailUri = Tools.convertBitmapToUri(activity!!, thumbnail!!)

        // Create thumbnail for the image which will also be sent to the backend
        thumbnailUris.add(thumbnailUri)
        currentMediaLocation.add(bitmapUri)
        tempFilesToCreate.add(TempUri(thumbnailUri, UploadMimeTypes.MEDIA))
    }

    private fun sendFile() {
        if (tempFilesToCreate.isNotEmpty()) {
            for (tempFile in tempFilesToCreate) {
                if (UploadMimeTypes.MEDIA == tempFile.type) {
                    createTempMediaMessage(tempFile.uri)
                } else if (UploadMimeTypes.FILE == tempFile.type) {
                    createTempFileMessage(tempFile.uri)
                }
            }

            tempFilesToCreate.clear()

            if (!uploadInProgress && unsentMessages.isNotEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (unsentMessages.first().type == Const.JsonFields.IMAGE_TYPE) {
                        uploadImage()
                    } else if (unsentMessages.first().type == Const.JsonFields.VIDEO_TYPE) {
                        uploadVideo()
                    } else if (filesSelected.isNotEmpty()) {
                        uploadFile()
                    }
                }, 2000)
            }
        }
    }

    /**
     * Update progress bar in recycler view
     * get viewHolder from position and progress bar from that viewHolder
     *  we are rapidly updating progressbar so we didn't use notify method as it always update whole row instead of only progress bar
     *  @param progress : new progress value
     */
    private fun updateUploadProgressBar(progress: Int, maxProgress: Int, localId: String) {
        // TODO check this method in the future. Upload is glitching sometimes and scrolling is lagging
        val message = messagesRecords.firstOrNull { it.message.localId == localId }
        message!!.message.uploadProgress = (progress * 100) / maxProgress

        activity!!.runOnUiThread { chatAdapter.notifyItemChanged(messagesRecords.indexOf(message)) }
    }

    /**
     * Method creates a random Integer from its lowest value to 0. This is to ensure that the
     * message in question is a temporary message, since all real messages have positive values.
     * After creating a random value, the method will check the current list containing unsent
     * messages and see if any of the items currently have the random number designated. This
     * ensures that no two unsent messages have the same id value. If it finds the same value
     * in the list, it will generate a new value and goi through the check again.
     **/
    private fun getUniqueRandomId(): Int {
        var randomId = Tools.generateRandomInt()

        while (unsentMessages.any { randomId == it.id }) {
            randomId = Tools.generateRandomInt()
        }

        return randomId
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
        if (uploadInProgress) {
            showUploadError(getString(R.string.upload_in_progress), true)
        } else {
            viewModel.updateUnreadCount(roomId = roomWithUsers.room.roomId)
            activity!!.finish()
        }
    }

    override fun onBackPressed(): Boolean {
        if (uploadInProgress) {
            showUploadError(getString(R.string.upload_in_progress), true)
            return false
        }

        for (bottomSheet in bottomSheets) {
            if (bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                return false
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        firstEnter = args.scrollDown

        for (bottomSheet in bottomSheets) {
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bindingSetup.clBottomMessageActions.visibility = View.GONE
        bindingSetup.clBottomSheet.visibility = View.GONE
        bindingSetup.clDetailsAction.visibility = View.GONE
        bindingSetup.clBottomReplyAction.visibility = View.GONE
        bindingSetup.clReactionsDetails.visibility = View.GONE

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