package com.clover.studio.spikamessenger.ui.main.chat.media

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.FragmentMediaBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.AppPermissions
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Const.MediaActions.Companion.MEDIA_DOWNLOAD
import com.clover.studio.spikamessenger.utils.Const.MediaActions.Companion.MEDIA_SHOW_BARS
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Resource
import java.io.File

const val BAR_ANIMATION = 500L

class MediaFragment : BaseFragment() {

    private var bindingSetup: FragmentMediaBinding? = null
    private val binding get() = bindingSetup!!
    private val viewModel: ChatViewModel by activityViewModels()
    private val args: MediaFragmentArgs by navArgs()

    private var message: Message? = null
    private var roomId: Int = 0
    private var roomUsers: List<User>? = null
    private var localUserId: Int = 0
    private var isFetching = false

    private var mediaList: List<Message> = arrayListOf()

    private var viewPager: ViewPager2? = null
    private var mediaPagerAdapter: MediaPagerAdapter? = null
    private var smallMediaAdapter: SmallMediaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        message = args.message
        roomId = message?.roomId ?: 0
        roomUsers = args.roomWithUsers?.users
        localUserId = args.localUserId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        initializeListeners()
        getAllPhotos()
        message?.let { setMediaInfo(it) }

        return binding.root
    }

    private fun initializeListeners() = with(binding) {
        ivBackToChat.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        ivMoreMedia.setOnClickListener {
            ChooserDialog.getInstance(
                requireContext(),
                null,
                null,
                getString(R.string.save),
                null,
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        downloadMedia(mediaList[viewPager.currentItem])
                    }

                    override fun onSecondOptionClicked() {
                        // Ignore
                    }
                }
            )
        }
    }

    private fun getAllPhotos() {
        viewModel.getAllMediaWithOffset(roomId = roomId).observe(viewLifecycleOwner) {
            if (Resource.Status.SUCCESS == it.status) {
                if (it.responseData != null) {
                    mediaList = it.responseData

                    if (!mediaList.contains(message)) {
                        viewModel.fetchNextMediaSet(roomId)
                    } else {
                        initializePagerAdapter()
                        setUpSmallMediaAdapter()
                    }
                }
            }
        }
    }

    private fun initializePagerAdapter() {
        mediaPagerAdapter =
            MediaPagerAdapter(requireContext(), mediaList, onItemClicked = { event, message ->
                when (event) {
                    MEDIA_SHOW_BARS -> showBar()
                    MEDIA_DOWNLOAD -> downloadMedia(message)
                }
            })

        viewPager?.adapter = mediaPagerAdapter
        binding.viewPager.adapter = mediaPagerAdapter

        binding.viewPager.setCurrentItem(mediaList.indexOf(message), false)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setUpSelectedSmallMedia(mediaList[position])

                message = mediaList[position]

                if (position == smallMediaAdapter?.itemCount) {
                    viewModel.fetchNextMediaSet(roomId = roomId)
                }

                setMediaInfo(mediaList[position])
            }
        })

    }

    private fun setMediaInfo(chatMessage: Message) {
        val mediaInfo: String = if (chatMessage.fromUserId == localUserId) {
            requireContext().getString(
                R.string.you_sent_on,
                Tools.fullDateFormat(chatMessage.createdAt!!)
            )
        } else {
            val userName =
                roomUsers?.firstOrNull { it.id == chatMessage.fromUserId }?.formattedDisplayName
            requireContext().getString(
                R.string.user_sent_on,
                userName,
                Tools.fullDateFormat(chatMessage.createdAt ?: 0L)
            )
        }
        binding.tvMediaInfo.text = mediaInfo
    }

    private fun setUpSmallMediaAdapter() {
        smallMediaAdapter = SmallMediaAdapter(context = requireContext()) {
            binding.viewPager.setCurrentItem(mediaList.indexOf(it), false)
        }

        val linearLayoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        binding.rvSmallMedia.apply {
            itemAnimator = null
            adapter = smallMediaAdapter
            layoutManager = linearLayoutManager

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition()
                    val totalItemCount = linearLayoutManager.itemCount

                    if (lastVisiblePosition == totalItemCount - 1 && !isFetching) {

                        viewModel.fetchNextMediaSet(roomId = roomId)
                        isFetching = true

                    } else if (lastVisiblePosition != totalItemCount - 1) {
                        isFetching = false
                    }
                }
            })
        }

        message?.let { setUpSelectedSmallMedia(it) }
    }

    private fun setUpSelectedSmallMedia(selectedMessage: Message) {
        smallMediaAdapter?.apply {
            setSelectedSmallMedia(mediaList.indexOf(selectedMessage))
            submitList(mediaList)
        }
        binding.rvSmallMedia.scrollToPosition(mediaList.indexOf(selectedMessage))
    }

    private fun showBar() = with(binding) {
        val showBars =
            clTopBar.visibility == View.GONE && flBottomBar.visibility == View.GONE
        clTopBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                clTopBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()

        flBottomBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                flBottomBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()
    }

    private fun downloadMedia(message: Message) {
        val appName =
            context?.applicationInfo?.loadLabel(requireContext().packageManager).toString()
        val tmp = Tools.getFilePathUrl(message.body!!.fileId!!)
        val request = DownloadManager.Request(Uri.parse(tmp))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle(message.body.file?.fileName)
        request.setDescription(MainApplication.appContext.getString(R.string.file_is_downloading))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (AppPermissions.hasStoragePermission) {
                val subdirectory = File(requireContext().getExternalFilesDir(null), appName)
                if (!subdirectory.exists()) {
                    subdirectory.mkdirs()
                }
                request.setDestinationInExternalPublicDir(
                    subdirectory.path,
                    message.body.file!!.fileName
                )
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.storage_permission),
                    Toast.LENGTH_LONG
                ).show()
                Tools.navigateToAppSettings()
            }
        } else {
            Toast.makeText(
                context,
                getString(R.string.download_media),
                Toast.LENGTH_LONG
            ).show()
        }

        val manager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val uri = getDownloadedFileUri(requireContext(), downloadId)
                        uri?.let {
                            saveMedia(uri)
                        }
                    } else {
                        if (AppPermissions.hasStoragePermission) {
                            Toast.makeText(
                                context,
                                getString(R.string.saved_to_gallery),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                context?.unregisterReceiver(this)
            }
        }
        registerReceiver(
            requireContext(),
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_EXPORTED
        )
    }


    private fun getDownloadedFileUri(context: Context, downloadId: Long): Uri? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().apply {
            setFilterById(downloadId)
        }
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val uriString = cursor.getString(uriIndex)
                return Uri.parse(uriString)
            }
        }
        cursor?.close()
        return null
    }

    /** This method is for saving images and videos for Android versions above 10 **/
    private fun saveMedia(uri: Uri) {
        var appName =
            context?.applicationInfo?.loadLabel(requireContext().packageManager).toString() + " "
        var relativeLocation: String
        val mimetype: String
        val externalContent: Uri
        if (Const.JsonFields.IMAGE_TYPE == message?.type) {
            relativeLocation = Environment.DIRECTORY_PICTURES
            mimetype = Const.JsonFields.IMAGE_JPEG
            externalContent = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            appName += getString(R.string.images)
        } else {
            relativeLocation = Environment.DIRECTORY_MOVIES
            mimetype = Const.JsonFields.VIDEO_MP4
            externalContent = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            appName += getString(R.string.videos)
        }

        relativeLocation += File.separator + appName

        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, message?.body?.file?.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimetype)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        }

        val mediaUri = resolver.insert(externalContent, contentValues)
        mediaUri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        Toast.makeText(context, getString(R.string.saved_to_gallery), Toast.LENGTH_LONG).show()
    }
}


