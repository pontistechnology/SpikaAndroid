package com.clover.studio.spikamessenger.ui.main.chat.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.clover.studio.spikamessenger.databinding.FragmentMediaPreparationBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.VideoCompression
import timber.log.Timber

class MediaPreparationFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    private var bindingSetup: FragmentMediaPreparationBinding? = null
    private val binding get() = bindingSetup!!

//    private var emojiPopup: EmojiPopup? = null

    private var muteAudio = false
    private var uris: List<Uri>? = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaPreparationBinding.inflate(inflater, container, false)

        uris = viewModel.mediaUri.value

        initializeViews()

        // TODO This will be added when captioning gets added
//        emojiPopup = EmojiPopup(
//            rootView = binding.root,
//            editText = binding.etMessage,
//            theming = Tools.setEmojiViewTheme(requireContext())
//        )

        return binding.root
    }

    private fun initializeViews() = with(binding) {
        Timber.d("Media uris = $uris")
        ivFullImage.visibility = View.VISIBLE
        ivFullImage.setImageURI(uris?.first())

        ivCancel.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        ivVideoQuality.setOnClickListener {
            // TODO show bottom sheet with video quality options
        }
        ivMuteAudio.setOnClickListener {
            muteAudio = !muteAudio
        }

        // TODO This will be added when captioning gets added
//        ivBtnEmoji.setOnClickListener {
//            emojiPopup?.toggle()
//            emojiPopup?.dismiss()
//        }
        ivButtonSend.setOnClickListener {
            compressVideos()
        }

        // TODO Remove this when captioning gets added
        etMessage.isEnabled = false
    }

    private fun compressVideos() {
        try {
            uris?.let { mediaUris ->
                VideoCompression.compressVideoFile(
                    uris = mediaUris,
                    muteAudio = muteAudio,
                    videoQuality = VideoQuality.HIGH,
                    object : CompressionListener {
                        override fun onCancelled(index: Int) {
                            // TODO("Not yet implemented")
                        }

                        override fun onFailure(index: Int, failureMessage: String) {
                            // TODO("Not yet implemented")
                        }

                        override fun onProgress(index: Int, percent: Float) {
                            // TODO("Not yet implemented")
                        }

                        override fun onStart(index: Int) {
                            // TODO("Not yet implemented")
                        }

                        override fun onSuccess(index: Int, size: Long, path: String?) {
                            if (index == mediaUris.size - 1) {
                                // TODO set values and navigate back to fragment
                                Timber.d("File compression done")
                            } else {
                                // We still have compression to do
                                Timber.d("Compressed file at index: $index and path: $path")
                            }
                        }
                    })
            }
        } catch (ex: Exception) {
            Timber.d("Something went wrong ${ex.message}")
        }
    }
}
