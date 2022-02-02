package com.clover.studio.exampleapp.ui.onboarding.account_creation

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.networking.FileChunk
import com.clover.studio.exampleapp.databinding.FragmentAccountCreationBinding
import com.clover.studio.exampleapp.ui.main.startMainActivity
import com.clover.studio.exampleapp.ui.onboarding.OnboardingStates
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.EventObserver
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


const val chunkSize = 32000

class AccountCreationFragment : Fragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var currentPhotoLocation: Uri
    private var md5FileHash: String? = "";

    private val choosePhotoContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                Glide.with(this).load(it).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = it
                val inputStream =
                    requireActivity().contentResolver.openInputStream(currentPhotoLocation)
                md5FileHash = calculateMD5(copyStreamToFile(inputStream!!))
            }
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

        addTextListeners()
        addClickListeners()
        addObservers()

        viewModel.sendContacts()
        return binding.root
    }

    private fun addObservers() {
        viewModel.accountCreationListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                OnboardingStates.CONTACTS_SENT -> Timber.d("Contacts sent successfully")
                OnboardingStates.CONTACTS_ERROR -> Timber.d("Failed to send contacts")
                else -> Timber.d("Other error")
            }
        })

        viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                OnboardingStates.USER_UPDATED -> startMainActivity(requireActivity())
                OnboardingStates.USER_UPDATE_ERROR -> Timber.d("Error updating user")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun addClickListeners() {
        binding.btnNext.setOnClickListener {
            checkUsername()
        }

        binding.cvPhotoPicker.setOnClickListener {
            choosePhoto()
        }
    }

    private fun addTextListeners() {
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
    }

    private fun checkUsername() {
        if (binding.etEnterUsername.text.isNullOrEmpty()) {
            binding.btnNext.isEnabled = false
            binding.clUsernameError.visibility = View.VISIBLE
        } else {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)
            uploadFile(copyStreamToFile(inputStream!!))
//            viewModel.updateUserData(hashMapOf(Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString()))
        }
    }

    private fun choosePhoto() {
        choosePhotoContract.launch("image/*")
    }

    private fun uploadFile(file: File) {
        Timber.d("${file.length()}")
        val pieces = file.length() / chunkSize

        val stream = BufferedInputStream(FileInputStream(file))
        val buffer = ByteArray(chunkSize)

        for (i in 0 until pieces) {
            if (stream.read(buffer) == -1) break

//            val fileBytes = stream.readBytes()
            val base64 = Base64.encodeToString(buffer, Base64.DEFAULT)

            val fileChunk = FileChunk(
                base64,
                i,
                pieces,
                file.length(),
                "image/*",
                file.name.toString(),
                "SomeId",
                md5FileHash,
                "avatar",
                null
            )

            viewModel.uploadFile(fileChunk.chunkToJson())
        }
    }

    private fun copyStreamToFile(inputStream: InputStream): File {
        val outputFile = File(requireActivity().cacheDir, "tempFile.jpeg")
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
        return outputFile
    }

    private fun calculateMD5(updateFile: File?): String? {
        val digest: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            Timber.d("Exception while getting digest", e)
            return null
        }
        val inputStream: InputStream = try {
            FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            Timber.d("Exception while getting FileInputStream", e)
            return null
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum: ByteArray = digest.digest()
            val bigInt = BigInteger(1, md5sum)
            var output: String = bigInt.toString(16)
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0')
            output
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for MD5", e)
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                Timber.d("Exception on closing MD5 input stream", e)
            }
        }
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