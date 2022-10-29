package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.example.moza.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MassagesViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel() {

    val gettingMessagesResponse : MutableLiveData<Resource<MutableList<ChatUser>>> = MutableLiveData()
    val getUserByNameResponse : MutableLiveData<Resource<MutableList<User>>> = MutableLiveData()

    private val _hideChatResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val hideChatResponse : LiveData<Resource<String>> get() = _hideChatResponse

    private val _deleteChatResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteChatResponse : LiveData<Resource<String>> get() = _deleteChatResponse

    fun safeGetUserChats(userID : String) = viewModelScope.launch {

        getUserChats(userID)
    }

    private suspend fun getUserChats(userID: String) {
        repository.getUserAllChats(userID).collect { response->
            when(response){
                is Resource.Error -> {
                    gettingMessagesResponse.postValue(Resource.Error(response.message.toString()))
                }
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    gettingMessagesResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }


    fun safeGetUserByName(username : String) = viewModelScope.launch {
        getUserByName(username)
    }

    private suspend fun getUserByName(username: String) {
        repository.getUsersByName(username).collect{ response->
            when(response){
                is Resource.Error -> getUserByNameResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> getUserByNameResponse.postValue(Resource.Success(response.data!!))
            }

        }
    }

    fun safeHideChat(chatId : String , userID: String) = viewModelScope.launch {
        hideChat(chatId , userID)
    }

    private suspend fun hideChat(chatId: String, userID: String) {
        repository.hideChat(chatId , userID).collect{ response->
            when(response){
                is Resource.Error -> _hideChatResponse.postValue(Resource.Success(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _hideChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeShowChat(chatId : String , hideIDS: ArrayList<String>) = viewModelScope.launch {
        showChat(chatId , hideIDS)
    }

    private suspend fun showChat(chatId: String, hideIDS: ArrayList<String>) {
        repository.showChat(chatId , hideIDS).collect{ response->
            when(response){
                is Resource.Error -> Log.e("Showing chat: " , response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Showing chat: " , response.data!!)
            }
        }
    }

    fun safeDeleteChat(chatId : String , userID: String) = viewModelScope.launch {
        deleteChat(chatId , userID)
    }

    private suspend fun deleteChat(chatId: String, userID: String) {
        repository.deleteChat(chatId , userID).collect{ response->
            when(response){
                is Resource.Error -> _deleteChatResponse.postValue(Resource.Success(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _deleteChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeUnDeleteChat(chatId : String , hideIDS: ArrayList<String>) = viewModelScope.launch {
        unDeleteChat(chatId , hideIDS)
    }

    private suspend fun unDeleteChat(chatId: String, hideIDS: ArrayList<String>) {
        repository.unDeleteChat(chatId , hideIDS).collect{ response->
            when(response){
                is Resource.Error -> Log.e("Showing chat: " , response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Showing chat: " , response.data!!)
            }
        }
    }

}
