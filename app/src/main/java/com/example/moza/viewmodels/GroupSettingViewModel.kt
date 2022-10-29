package com.example.moza.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupSettingViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel() {
    private val _getChatResponse: MutableLiveData<Resource<ChatUser>> = MutableLiveData()
    val getChatResponse: LiveData<Resource<ChatUser>> get() = _getChatResponse

    private val _getUsersDataResponse: MutableLiveData<Resource<MutableList<User>>> =
        MutableLiveData()
    val getUsersDataResponse: LiveData<Resource<MutableList<User>>> get() = _getUsersDataResponse

    private val _updateGroupResponse: MutableLiveData<Resource<String>> =
        MutableLiveData()
    val updateGroupResponse: LiveData<Resource<String>> get() = _updateGroupResponse

    private val _updateGroupImg: MutableLiveData<Resource<String>> =
        MutableLiveData()
    val updateGroupImgResponse: LiveData<Resource<String>> get() = _updateGroupImg

    fun safeGetChat(chatID: String) = viewModelScope.launch {
        getChat(chatID)
    }

    private suspend fun getChat(chatID: String) {
        repository.getChat(chatID).collect { response ->
            when (response) {
                is Resource.Error -> _getChatResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _getChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun getUsers(usersIDS : ArrayList<String>) = viewModelScope.launch {
        repository.getUsers(usersIDS).collect{ response->
            when(response){
                is Resource.Error -> _getUsersDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _getUsersDataResponse.postValue(Resource.Loading())
                is Resource.Success ->  _getUsersDataResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun updateGroupData(chatID: String , chatUser: ChatUser) = viewModelScope.launch {
        repository.updateGroupData(chatID, chatUser).collect{response->
            when(response){
                is Resource.Error -> _updateGroupResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _updateGroupResponse.postValue(Resource.Loading())
                is Resource.Success ->  _updateGroupResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun updateGroupImg(chatID: String , uri : Uri) = viewModelScope.launch {
        repository.updateGroupImage(chatID, uri).collect{response->
            when(response){
                is Resource.Error -> _updateGroupImg.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _updateGroupImg.postValue(Resource.Loading())
                is Resource.Success ->  _updateGroupImg.postValue(Resource.Success(response.data!!))
            }
        }
    }



}