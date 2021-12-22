package com.clover.studio.exampleapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.FragmentContactDetailsBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import kotlin.random.Random

class ContactDetailsFragment : Fragment() {
    private lateinit var user: User

    private var bindingSetup: FragmentContactDetailsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = if (requireArguments().getParcelable<User>(Const.Navigation.USER_PROFILE) == null) {
            User(
                1,
                "Matom",
                Tools.getRandomImageUrl(Random.nextInt(5)),
                "+384945556666",
                "someHash",
                "mojemail@lol.com",
                "Time"
            )
        } else {
            requireArguments().getParcelable(Const.Navigation.USER_PROFILE)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactDetailsBinding.inflate(inflater, container, false)

        binding.tvUsername.text = user.displayName

        Glide.with(this).load(user.avatarUrl).into(binding.ivPickAvatar)

        return binding.root
    }
}