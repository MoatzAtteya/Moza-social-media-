package com.example.moza.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageOptionViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel(){

    private val _deleteMessageResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteMessageResponse : LiveData<Resource<String>> get() = _deleteMessageResponse

    private val _editMessageResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val editMessageResponse : LiveData<Resource<String>> get() = _editMessageResponse

    private val _deleteLastMessageFlow = MutableStateFlow("")
    val deleteLastMessageFlow get() = _deleteLastMessageFlow.asStateFlow()

     fun safeDeleteMessage(messageID : String , chatID : String) = viewModelScope.launch {
        deleteMessage(messageID, chatID)
    }

    private suspend fun deleteMessage(messageID : String , chatID : String) {
        repository.deleteMessage(messageID, chatID).cancellable().collect { response ->
            when (response) {
                is Resource.Error -> {}
                is Resource.Success -> {
                    deleteLastMessage(chatID, messageID)
                }
                else -> {}
            }

        }
    }

     fun safeEditMessage(messageID: String, chatID: String, newText: String) =
        viewModelScope.launch {
            editMessage(messageID, chatID, newText)
        }

    private suspend fun editMessage(messageID: String , chatID: String , newText: String) {
        repository.editMessage(messageID,chatID,newText).cancellable().collect{ response->
            when (response) {
                is Resource.Error -> _editMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Success -> _editMessageResponse.postValue(Resource.Success(response.data!!))
                else -> {}
            }
        }
    }





    private suspend fun deleteLastMessage(chatID: String, messageID: String) {
        getChat(chatID , messageID)
    }

    private suspend fun getChat(chatID: String , messageID: String) {
        repository.getChat(chatID).collect { response ->
            when (response) {
                is Resource.Error -> {}
                is Resource.Loading -> {}
                is Resource.Success -> {
                    getMessageDocument(response.data!!,messageID)
                }
            }
        }
    }

    private suspend fun getMessageDocument(chat: ChatUser, messageID: String)  {
        repository.getMessage(messageID, chat.id!!).collect { response ->
            when (response) {
                is Resource.Error -> TODO()
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    val message = response.data
                    compareMessages(chat, message)
                }
            }
        }
    }

    private suspend fun compareMessages(chat: ChatUser, message: ChatMessage?) {
        if (chat.lastMessage_id == message!!.id) {
            repository.deleteLastMessage(chat.id!!).collect { response ->
                when (response) {
                    is Resource.Error -> _deleteMessageResponse.postValue(Resource.Error(response.message!!))
                    is Resource.Loading -> TODO()
                    is Resource.Success -> _deleteMessageResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }

    }


}