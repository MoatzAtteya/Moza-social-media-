package com.example.moza.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.Story
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddStoryViewModel @Inject constructor(val repository: FireBaseRepository ,val firebaseUser: FirebaseUser) : ViewModel() {

    val uploadUserStory: MutableLiveData<Resource<String>> = MutableLiveData()
    val getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()

    fun safeUploadUserStory(uri: Uri, story: Story) = viewModelScope.launch {
        var filename = ""
        if (uri.toString().contains("image"))
            filename = "${System.currentTimeMillis()}.png"
        else if (uri.toString().contains("video"))
            filename = "${System.currentTimeMillis()}.video"


        uploadUserStory(uri, story,filename)
    }

    private suspend fun uploadUserStory(uri: Uri, story: Story, filename: String) {
        repository.uploadUserStory(uri, story , filename).collect { response ->
            when (response) {
                is Resource.Error -> {
                    uploadUserStory.postValue(Resource.Error(response.message!!))
                }
                is Resource.Loading -> {
                    uploadUserStory.postValue(Resource.Loading())

                }
                is Resource.Success -> {
                    uploadUserStory.postValue(Resource.Success(response.data!!))

                }
            }
        }
    }

    fun safeGetUserData() = viewModelScope.launch {
        getUserData()
    }

    private suspend fun getUserData() {
        repository.getUserData2(firebaseUser.uid).collect { response ->
            when(response){
                is Resource.Error -> {
                    getUserDataResponse.postValue(Resource.Error(response.message.toString()))
                }
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    getUserDataResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }

}

