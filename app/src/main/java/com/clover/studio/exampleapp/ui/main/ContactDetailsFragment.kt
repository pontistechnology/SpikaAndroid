package com.clover.studio.exampleapp.ui.main

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.FragmentContactDetailsBinding
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.gson.Gson
import timber.log.Timber

class ContactDetailsFragment : BaseFragment() {
    private var user: User? = null

    private var bindingSetup: FragmentContactDetailsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireArguments().getParcelable<User>(Const.Navigation.USER_PROFILE) == null) {
            DialogError.getInstance(requireActivity(),
                getString(R.string.error),
                getString(R.string.failed_user_data),
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
            Timber.d("Failed to fetch user data")
        } else {
            user = requireArguments().getParcelable(Const.Navigation.USER_PROFILE)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactDetailsBinding.inflate(inflater, container, false)

        initializeViews()
        return binding.root
    }

    private fun initializeViews() {
        if (user != null) {
            binding.tvUsername.text = user?.displayName
            binding.tvPageName.text = user?.displayName

            binding.ivChat.setOnClickListener {
                val gson = Gson()
                val userData = gson.toJson(user)
                startChatScreenActivity(requireActivity(), userData)
            }

            Glide.with(this).load(user?.avatarUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.d("Failed to load user image")
                        DialogError.getInstance(requireActivity(),
                            getString(R.string.error),
                            getString(R.string.failed_user_image),
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
                        binding.clProgressScreen.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.d("Image loaded successfully")
                        binding.clProgressScreen.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.ivPickAvatar)
        }

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}