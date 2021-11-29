package com.clover.studio.exampleapp.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.clover.studio.exampleapp.R

class MainPagerAdapter constructor(
    private val context: Context,
    private val fragments: ArrayList<Fragment>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val imageResources = arrayOf(
        R.drawable.nav_contact_states,
        R.drawable.nav_call_history_states,
        R.drawable.nav_chat_states,
        R.drawable.nav_settings_states
    )

    fun getTabView(position: Int): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.custom_tab_layout, null)
        val imageView = view.findViewById<ImageView>(R.id.iv_custom_tab)

        imageView.setImageResource(
            imageResources[position]
        )

        return view
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}