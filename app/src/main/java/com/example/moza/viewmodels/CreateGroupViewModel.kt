package com.example.moza.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    val repository: FireBaseRepository,
    val firebaseUser: FirebaseUser
) : ViewModel() {

    private val _getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()
    val getUserDataResponse: LiveData<Resource<User>> get() = _getUserDataResponse

    private val _getUsersDataResponse: MutableLiveData<Resource<MutableList<User>>> = MutableLiveData()
    val getUsersDataResponse: LiveData<Resource<MutableList<User>>> get() = _getUsersDataResponse

    private val _startGroupChat: MutableLiveData<Resource<String>> = MutableLiveData()
    val startGroupChat: LiveData<Resource<String>> get() = _startGroupChat


    fun getUserData() = viewModelScope.launch {
        repository.getUserData2(firebaseUser.uid).collect { response->
            when (response) {
                is Resource.Error -> _getUserDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _getUserDataResponse.postValue(Resource.Success(response.data!!))
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

    fun safeStartChat(chatUser: ChatUser) = viewModelScope.launch {
        chatUser.uid.add(firebaseUser.uid)
        println("id list is: ${chatUser.uid}")
        startChat(chatUser)
    }

    private suspend fun startChat(chatUser: ChatUser) {
        repository.createGroupChat(chatUser).collect { response ->
            when (response) {
                is Resource.Error -> _startGroupChat.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _startGroupChat.postValue(Resource.Loading())
                is Resource.Success -> _startGroupChat.postValue(Resource.Success(response.data!!))
            }
        }
    }

}