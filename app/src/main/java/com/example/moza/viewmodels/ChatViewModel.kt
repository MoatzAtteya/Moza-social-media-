package com.example.moza.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.*
import kotlinx.coroutines.launch

class ChatViewModel() : ViewModel() {

    val repository: FireBaseRepository = FireBaseRepository()

    val getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()
    val getChatMessagesResponse: MutableLiveData<Resource<MutableList<ChatMessage>>> =
        MutableLiveData()
    val sendMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendUriMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()

    private val _getChatResponse: MutableLiveData<Resource<ChatUser>> = MutableLiveData()
    val getChatResponse: LiveData<Resource<ChatUser>> get() = _getChatResponse

    private val _deleteChatResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteChatResponse: LiveData<Resource<String>> get() = _deleteChatResponse

    private val _sendVoiceMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendVoiceMessageResponse: LiveData<Resource<String>> get() = _sendVoiceMessageResponse


    fun safeGetUserData(userId: String) = viewModelScope.launch {
        getUserData(userId)
    }

    private suspend fun getUserData(userId: String) {
        repository.getUserData2(userId).collect { response ->
            when (response) {
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

    fun safeGetChatMessages(chatID: String) = viewModelScope.launch {
        getChatMessages(chatID)
    }

    private suspend fun getChatMessages(chatID: String) {
        repository.getChatMessages(chatID).collect { response ->
            when (response) {
                is Resource.Error -> {
                    getChatMessagesResponse.postValue(Resource.Error(response.message.toString()))
                }
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    getChatMessagesResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }

    fun safeSendMessage(chatID: String, message: ChatMessage, userId: String) =
        viewModelScope.launch {
            sendMessage(chatID, message, userId)
        }

    private suspend fun sendMessage(chatID: String, message: ChatMessage, userId: String) {

        repository.sendMessage(chatID, message, userId).collect { response ->
            when (response) {
                is Resource.Error -> sendMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Success -> sendMessageResponse.postValue(Resource.Success(response.data!!))
                else -> {
                    return@collect
                }
            }
        }
    }

    fun safeSendUriMessage(chatID: String, uri: Uri, message: ChatMessage, userId: String) =
        viewModelScope.launch {
            var filename = ""
            filename = if (uri.toString().contains("image"))
                "${System.currentTimeMillis()}.png"
            else if (uri.toString().contains("video"))
                "${System.currentTimeMillis()}.video"
            else if (uri.toString().contains("mp3"))
                "${System.currentTimeMillis()}.mp3"
            else
                ""

            sendUriMessage(chatID, uri, message, filename, userId)

        }

    private suspend fun sendUriMessage(
        chatID: String,
        uri: Uri,
        message: ChatMessage,
        filename: String,
        userId: String
    ) {
        repository.sendUriMessage(chatID, message, uri, filename, userId).collect { response ->
            when (response) {
                is Resource.Error -> sendUriMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Success -> sendUriMessageResponse.postValue(Resource.Success(response.data!!))
                is Resource.Loading -> sendUriMessageResponse.postValue(Resource.Loading())
            }
        }


    }


    fun sendNotification(notification: PushNotification) = viewModelScope.launch {
        try {
            val response = NotificationRetrofitObj.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d("notification:", "notification sent!")
            } else {
                Log.e("error in sending notification", response.errorBody()!!.string())
            }
        } catch (e: Exception) {
            Log.e("error in sending notification", e.toString())
        }
    }


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

    fun safeDeleteChat(chatID: String) = viewModelScope.launch {
        deleteChat(chatID)
    }

    private suspend fun deleteChat(chatID: String) {
        repository.deleteChatPermanently(chatID).collect { response ->
            when (response) {
                is Resource.Error -> _deleteChatResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _deleteChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeMarkChatASRead(chatID: String) = viewModelScope.launch {
        markChatAsRead(chatID)
    }

    private suspend fun markChatAsRead(chatID: String) {
        repository.markChatAsRead(chatID).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Marking chat as read: ", response.message!!)
                is Resource.Loading -> return@collect
                is Resource.Success -> Log.d("Marking chat as read: ", response.data!!)
            }
        }
    }

    fun safeSendVoiceMessage(chatID: String, message: ChatMessage, uri: Uri, filename: String) =
        viewModelScope.launch {
            sendVoiceMessage(chatID, message, uri, filename)
        }

    private suspend fun sendVoiceMessage(
        chatID: String,
        message: ChatMessage,
        uri: Uri,
        filename: String
    ) {
        repository.sendVoiceMessage(chatID, message, uri, filename).collect { response ->
            when (response) {
                is Resource.Error -> _sendVoiceMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _sendVoiceMessageResponse.postValue(Resource.Loading())
                is Resource.Success -> _sendVoiceMessageResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}
