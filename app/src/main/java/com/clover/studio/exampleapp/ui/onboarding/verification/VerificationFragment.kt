package com.clover.studio.exampleapp.ui.onboarding.verification

import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentVerificationBinding
import com.clover.studio.exampleapp.ui.main.startMainActivity
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.SmsListener
import com.clover.studio.exampleapp.utils.SmsReceiver
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.gson.JsonObject
import timber.log.Timber
import java.util.concurrent.TimeUnit


class VerificationFragment : BaseFragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var phoneNumber: String
    private lateinit var phoneNumberHashed: String
    private lateinit var countryCode: String
    private lateinit var deviceId: String
    private lateinit var intentFilter: IntentFilter
    private lateinit var smsReceiver: SmsReceiver
    private lateinit var timer: CountDownTimer

    private var bindingSetup: FragmentVerificationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneNumber = requireArguments().getString(Const.Navigation.PHONE_NUMBER).toString()
        phoneNumberHashed =
            requireArguments().getString(Const.Navigation.PHONE_NUMBER_HASHED).toString()
        countryCode = requireArguments().getString(Const.Navigation.COUNTRY_CODE).toString()
        deviceId = requireArguments().getString(Const.Navigation.DEVICE_ID).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentVerificationBinding.inflate(inflater, container, false)

        binding.tvEnterNumber.text = getString(R.string.verification_code_sent, phoneNumber)

        setupTextWatchers()
        startSmsRetriever()
        initBroadCast()
        setClickListeners()
        setObservers()
        initCountdownTimer()

        return binding.root
    }

    private fun initCountdownTimer() {
        timer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished).toString()
                var timeInSeconds =
                    (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60).toString()

                if (timeInMinutes.length < 2) {
                    timeInMinutes = "0$timeInMinutes"
                }

                if (timeInSeconds.length < 2) {
                    timeInSeconds = "0$timeInSeconds"
                }
                binding.tvTimer.text = "$timeInMinutes:$timeInSeconds"
            }

            override fun onFinish() {
                binding.tvTimer.text = getString(R.string.timeout)
            }
        }
        timer.start()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(smsReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(smsReceiver)
    }

    private fun setObservers() {
        viewModel.codeVerificationListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.LOADING -> {
                    binding.clInputUi.visibility = View.GONE
                    binding.ivSpikaVerify.visibility = View.VISIBLE
                }
                Resource.Status.SUCCESS -> {
                    binding.ivSpikaVerify.setImageResource(R.drawable.img_logo_empty)
                    binding.ivCheckmark.visibility = View.VISIBLE
                    goToMainActivity()
                }
                Resource.Status.NEW_USER -> {
                    binding.ivSpikaVerify.setImageResource(R.drawable.img_logo_empty)
                    binding.ivCheckmark.visibility = View.VISIBLE
                    goToAccountCreation()
                }
                Resource.Status.ERROR -> {
                    binding.ivSpikaVerify.visibility = View.GONE
                    binding.clInputUi.visibility = View.VISIBLE
                    binding.tvIncorrectCode.visibility = View.VISIBLE
                }
                else -> Timber.d("Something went wrong")
            }
        })
    }

    private fun setClickListeners() {
        binding.btnNext.setOnClickListener {
            viewModel.sendCodeVerification(getVerificationJsonObject())
        }

        binding.tvResendCode.setOnClickListener {
            viewModel.sendNewUserData(getPhoneJsonObject())
        }
    }

    private fun getVerificationJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(Const.JsonFields.CODE, getVerificationCode())
        jsonObject.addProperty(Const.JsonFields.DEVICE_ID, deviceId)

        return jsonObject
    }

    private fun getPhoneJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(Const.JsonFields.TELEPHONE_NUMBER, phoneNumber)
        jsonObject.addProperty(Const.JsonFields.TELEPHONE_NUMBER_HASHED, phoneNumberHashed)
        jsonObject.addProperty(Const.JsonFields.COUNTRY_CODE, countryCode)
        jsonObject.addProperty(Const.JsonFields.DEVICE_ID, deviceId)

        return jsonObject
    }

    private fun initBroadCast() {
        intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        smsReceiver = SmsReceiver()
        smsReceiver.bindListener(object : SmsListener {
            override fun messageReceived(messageText: String?) {
                Timber.d("MESSAGE received $messageText")
                binding.etInputOne.setText(messageText?.get(0).toString())
                binding.etInputTwo.setText(messageText?.get(1).toString())
                binding.etInputThree.setText(messageText?.get(2).toString())
                binding.etInputFour.setText(messageText?.get(3).toString())
                binding.etInputFive.setText(messageText?.get(4).toString())
                binding.etInputSix.setText(messageText?.get(5).toString())
            }
        })
    }

    private fun goToMainActivity() {
        val timer = object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("Timer tick $millisUntilFinished")
            }

            override fun onFinish() {
                startMainActivity(requireActivity())
            }
        }
        timer.start()
    }

    private fun goToAccountCreation() {
        val timer = object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("Timer tick $millisUntilFinished")
            }

            override fun onFinish() {
                findNavController().navigate(R.id.action_verificationFragment_to_accountCreationFragment)
            }
        }
        timer.start()
    }

    private fun setupTextWatchers() {
        //GenericTextWatcher here works only for moving to next EditText when a number is entered
        //first parameter is the current EditText and second parameter is next EditText
        binding.etInputOne.addTextChangedListener(
            GenericTextWatcher(
                binding.etInputOne,
                binding.etInputTwo
            )
        )
        binding.etInputTwo.addTextChangedListener(
            GenericTextWatcher(
                binding.etInputTwo,
                binding.etInputThree
            )
        )
        binding.etInputThree.addTextChangedListener(
            GenericTextWatcher(
                binding.etInputThree,
                binding.etInputFour
            )
        )
        binding.etInputFour.addTextChangedListener(
            GenericTextWatcher(
                binding.etInputFour,
                binding.etInputFive
            )
        )
        binding.etInputFive.addTextChangedListener(
            GenericTextWatcher(
                binding.etInputFive,
                binding.etInputSix
            )
        )
        binding.etInputSix.addTextChangedListener(GenericTextWatcher(binding.etInputSix, null))

        //GenericKeyEvent here works for deleting the element and to switch back to previous EditText
        //first parameter is the current EditText and second parameter is previous EditText
        binding.etInputOne.setOnKeyListener(GenericKeyEvent(binding.etInputOne, null))
        binding.etInputTwo.setOnKeyListener(GenericKeyEvent(binding.etInputTwo, binding.etInputOne))
        binding.etInputThree.setOnKeyListener(
            GenericKeyEvent(
                binding.etInputThree,
                binding.etInputTwo
            )
        )
        binding.etInputFour.setOnKeyListener(
            GenericKeyEvent(
                binding.etInputFour,
                binding.etInputThree
            )
        )
        binding.etInputFive.setOnKeyListener(
            GenericKeyEvent(
                binding.etInputFive,
                binding.etInputFour
            )
        )
        binding.etInputSix.setOnKeyListener(
            GenericKeyEvent(
                binding.etInputSix,
                binding.etInputFive
            )
        )

        binding.etInputOne.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputTwo.hasFocus() && !binding.etInputThree.hasFocus() && !binding.etInputFour.hasFocus() && !binding.etInputFive.hasFocus() && !binding.etInputSix.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }

        binding.etInputTwo.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputOne.hasFocus() && !binding.etInputThree.hasFocus() && !binding.etInputFour.hasFocus() && !binding.etInputFive.hasFocus() && !binding.etInputSix.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }

        binding.etInputThree.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputOne.hasFocus() && !binding.etInputTwo.hasFocus() && !binding.etInputFour.hasFocus() && !binding.etInputFive.hasFocus() && !binding.etInputSix.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }

        binding.etInputFour.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputOne.hasFocus() && !binding.etInputTwo.hasFocus() && !binding.etInputThree.hasFocus() && !binding.etInputFive.hasFocus() && !binding.etInputSix.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }

        binding.etInputFive.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputOne.hasFocus() && !binding.etInputTwo.hasFocus() && !binding.etInputThree.hasFocus() && !binding.etInputFour.hasFocus() && !binding.etInputSix.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }

        binding.etInputSix.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus && !binding.etInputOne.hasFocus() && !binding.etInputTwo.hasFocus() && !binding.etInputThree.hasFocus() && !binding.etInputFour.hasFocus() && !binding.etInputFive.hasFocus()) {
                    hideKeyboard(view)
                }
            }
        }
    }

    private fun startSmsRetriever() {
        // Get an instance of SmsRetrieverClient, used to start listening for a matching
        // SMS message.
        val client = SmsRetriever.getClient(requireActivity())

        // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
        // (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
        // action SmsRetriever#SMS_RETRIEVED_ACTION.
        val task = client.startSmsRetriever()

        // Listen for success/failure of the start Task. If in a background thread, this
        // can be made blocking using Tasks.await(task, [timeout]);
        task.addOnSuccessListener {
            // Successfully started retriever, expect broadcast intent
            Timber.d("MESSAGE success")
        }

        task.addOnFailureListener {
            // Failed to start retriever, inspect Exception for more details
            Timber.d("MESSAGE failed ${it.message}")
        }
    }

    private fun getVerificationCode(): String = binding.etInputOne.text.toString() +
            binding.etInputTwo.text.toString() +
            binding.etInputThree.text.toString() +
            binding.etInputFour.text.toString() +
            binding.etInputFive.text.toString() +
            binding.etInputSix.text.toString()

    inner class GenericKeyEvent internal constructor(
        private val currentView: EditText,
        private val previousView: EditText?
    ) : View.OnKeyListener {
        override fun onKey(p0: View?, keyCode: Int, event: KeyEvent?): Boolean {
            if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && currentView.id != binding.etInputOne.id && currentView.text.isEmpty()) {
                //If current is empty then previous EditText's number will also be deleted
                previousView?.text = null
                previousView?.requestFocus()
                return true
            }
            return false
        }
    }

    inner class GenericTextWatcher internal constructor(
        private val currentView: View,
        private val nextView: View?
    ) :
        TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            val text = editable.toString()
            when (currentView.id) {
                binding.etInputOne.id -> if (text.length == 1) nextView!!.requestFocus()
                binding.etInputTwo.id -> if (text.length == 1) nextView!!.requestFocus()
                binding.etInputThree.id -> if (text.length == 1) nextView!!.requestFocus()
                binding.etInputFour.id -> if (text.length == 1) nextView!!.requestFocus()
                binding.etInputFive.id -> if (text.length == 1) nextView!!.requestFocus()
                binding.etInputSix.id -> if (text.length == 1) {

                    hideKeyboard(binding.etInputSix)
                    val timer = object : CountDownTimer(500, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            Timber.d("Timer tick $millisUntilFinished")
                        }

                        override fun onFinish() {
                            viewModel.sendCodeVerification(getVerificationJsonObject())
                        }
                    }
                    timer.start()
                }
            }
        }

        override fun beforeTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) { // TODO Auto-generated method stub
        }

        override fun onTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int
        ) { // TODO Auto-generated method stub
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
    }
}