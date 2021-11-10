package com.clover.studio.exampleapp.ui.onboarding.verification

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentVerificationBinding
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.SmsListener
import com.clover.studio.exampleapp.utils.SmsReceiver
import timber.log.Timber


class VerificationFragment : Fragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var phoneNumber: String
    private lateinit var deviceId: String

    private var bindingSetup: FragmentVerificationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneNumber = requireArguments().getString(Const.Navigation.PHONE_NUMBER).toString()
        deviceId = requireArguments().getString(Const.Navigation.DEVICE_ID).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentVerificationBinding.inflate(inflater, container, false)

        binding.tvEnterNumber.text = getString(R.string.verification_code_sent, phoneNumber)

        setupTextWatchers()

        binding.btnNext.setOnClickListener {
            viewModel.sendCodeVerification(getVerificationCode(), deviceId)
        }

        SmsReceiver.bindListener(object : SmsListener {
            override fun messageReceived(messageText: String?) {
                // TODO set message text to fields
                Timber.d("SmsReceiver message $messageText")
            }
        })
        return binding.root
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
            if (event!!.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && currentView.id != binding.etInputOne.id && currentView.text.isEmpty()) {
                //If current is empty then previous EditText's number will also be deleted
                previousView!!.text = null
                previousView.requestFocus()
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
                    val imm =
                        activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etInputSix.windowToken, 0)
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
}