package com.example.moza.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.ChatUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel() {

    private val _deleteChatResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteChatResponse : LiveData<Resource<String>> get() = _deleteChatResponse

    private val _getChatResponse : MutableLiveData<Resource<ChatUser>> = MutableLiveData()
    val getChatResponse : LiveData<Resource<ChatUser>> get() = _getChatResponse

    fun safeRequestChatDelete(chatID :String , userID : String) = viewModelScope.launch {
        requestChatDelete(chatID , userID)
    }

    private suspend fun requestChatDelete(chatID: String, userID : String) {
        repository.requestDeleteChat(chatID, userID).collect{ response->
            when(response){
                is Resource.Error -> _deleteChatResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _deleteChatResponse.postValue(Resource.Loading())
                is Resource.Success -> _deleteChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeGetChat(chatID: String) = viewModelScope.launch {
        getChat(chatID)
    }

    private suspend fun getChat(chatID: String) {
        repository.getChat(chatID).collect{response->
            when(response){
                is Resource.Error -> _getChatResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _getChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

}