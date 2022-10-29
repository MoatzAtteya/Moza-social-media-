package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeActivityViewModel @Inject constructor(val repository: FireBaseRepository , val firebaseUser: FirebaseUser) : ViewModel() {


    fun safeUpdateUserStatus(type : Boolean) = viewModelScope.launch {
        updateUserStatus(firebaseUser.uid , type)
    }

    private suspend fun updateUserStatus(userId: String, type: Boolean) {
        repository.updateUserOnlineStatus(userId, type).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Error while changing user status: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Changing user status: ", response.data!!)
            }
        }
    }
}