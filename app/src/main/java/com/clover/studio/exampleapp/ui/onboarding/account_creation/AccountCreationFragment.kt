package com.clover.studio.exampleapp.ui.onboarding.account_creation

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.databinding.FragmentAccountCreationBinding
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import timber.log.Timber

class AccountCreationFragment : Fragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var currentPhotoLocation: Uri

    private val choosePhotoContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            Glide.with(this).load(it).into(binding.ivPickPhoto)
            binding.clSmallCameraPicker.visibility = View.VISIBLE
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                Glide.with(this).load(currentPhotoLocation).into(binding.ivPickPhoto)
            } else {
                Timber.d("Photo error")
            }
        }

    private var bindingSetup: FragmentAccountCreationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        bindingSetup = FragmentAccountCreationBinding.inflate(inflater, container, false)

        binding.etEnterUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // ignore
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.clUsernameError.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(text: Editable?) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.clUsernameError.visibility = View.INVISIBLE
                }
            }
        })

        binding.btnNext.setOnClickListener {
            checkUsername()
        }

        binding.cvPhotoPicker.setOnClickListener {
            choosePhoto()
        }
        return binding.root
    }

    private fun checkUsername() {
        if (binding.etEnterUsername.text.isNullOrEmpty()) {
            binding.btnNext.isEnabled = false
            binding.clUsernameError.visibility = View.VISIBLE
        } else {
            // TODO navigate forward
        }
    }

    private fun choosePhoto() {
        choosePhotoContract.launch("image/*")
    }

    // TODO take photo from camera logic
//    private fun takePhotoWithCamera() {
//        val tempPhoto: File? = Tools.makeTempFile(requireActivity())
//        if (tempPhoto != null) {
//            val currentPhotoPath = "file:${tempPhoto.absolutePath}"
//            takePhotoContract.launch(currentPhotoPath.toUri())
//            currentPhotoLocation = currentPhotoPath.toUri()
//        }
//    }
}