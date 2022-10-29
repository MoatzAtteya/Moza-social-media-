package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.User
import com.example.moza.common.Resource
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(val repository: FireBaseRepository , val firebaseUser: FirebaseUser): ViewModel()  {

    var updateUserResponse : MutableLiveData<Resource<String>> = MutableLiveData()

    fun safeUpdateUser(user: User) = viewModelScope.launch {
        updateUserData(user)
    }

    private suspend fun updateUserData(user: User) {
        repository.editProfile(user).collect{ response ->
            when(response){
                is Resource.Error -> updateUserResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> updateUserResponse.postValue(Resource.Loading())
                is Resource.Success -> updateUserResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeUpdateUserStatus() = viewModelScope.launch {
        repository.updateUserOnlineStatus(firebaseUser.uid, false).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Error while changing user status: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Changing user status: ", response.data!!)
            }
        }
    }

    fun updateProfilePrivacy(type : Boolean) = viewModelScope.launch {
        repository.updateProfilePrivacy(firebaseUser.uid , type).collect{ response->
            when (response) {
                is Resource.Error -> Log.e("Error while changing profile privacy: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Changing profile privacy: ", response.data!!)
            }
        }
    }


}