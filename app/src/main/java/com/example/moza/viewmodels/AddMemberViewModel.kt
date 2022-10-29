package com.example.moza.viewmodels

import android.util.Log
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
class AddMemberViewModel @Inject constructor(val firebaseUser: FirebaseUser , val repository: FireBaseRepository) : ViewModel() {

    private val _getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()
    val getUserDataResponse: LiveData<Resource<User>> get() = _getUserDataResponse

    private val _getUsersDataResponse: MutableLiveData<Resource<MutableList<User>>> = MutableLiveData()
    val getUsersDataResponse: LiveData<Resource<MutableList<User>>> get() = _getUsersDataResponse

    private val _addMembersResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val addMemberResponse : LiveData<Resource<String>> get() = _addMembersResponse



    fun getUserData() = viewModelScope.launch {
        repository.getUserData2(firebaseUser.uid).collect { response->
            when (response) {
                is Resource.Error -> _getUserDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
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

     fun getAndUpdateGroup(chatID: String , newMembers : ArrayList<String>) = viewModelScope.launch {
        repository.getChat(chatID).collect{response->
            when(response){
                is Resource.Error -> Log.e("Getting chat response: " , response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    updateGroupMembers(response.data!! , newMembers)
                }
            }
        }
    }

    private suspend fun updateGroupMembers(chat: ChatUser, newMembers: java.util.ArrayList<String>) {
        chat.uid.addAll(newMembers)
        repository.updateGroupData(chat.id!! , chat).collect{response->
            when(response){
                is Resource.Error -> _addMembersResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success ->  _addMembersResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}