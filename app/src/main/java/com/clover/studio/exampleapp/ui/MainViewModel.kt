package com.clover.studio.exampleapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.clover.studio.exampleapp.data.repositories.UserRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepositoryImpl
) : ViewModel() {
    fun getLocalUsers() = liveData {
        emitSource(repository.getUserLocal())
    }
}