package com.clover.studio.spikamessenger.ui.onboarding.terms_and_conditions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentTermsAndConditionsBinding
import com.clover.studio.spikamessenger.utils.Const

class TermsAndConditions : Fragment() {
    private var bindingSetup: FragmentTermsAndConditionsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentTermsAndConditionsBinding.inflate(inflater, container, false)

        initializeViews()

        return binding.root
    }

    private fun initializeViews() {
        binding.tvAgree.setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_registerNumberFragment)
        }

        val spannable = SpannableString(binding.tvWelcomeDescription.text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val uri =
                    Uri.parse(Const.Urls.TERMS_AND_CONDITIONS)

                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
        spannable.setSpan(
            ForegroundColorSpan(
                resources.getColor(
                    R.color.primary_color,
                    null
                )
            ), 35, binding.tvWelcomeDescription.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            clickableSpan,
            35,
            binding.tvWelcomeDescription.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvWelcomeDescription.text = spannable
        binding.tvWelcomeDescription.movementMethod = LinkMovementMethod.getInstance()
    }
}