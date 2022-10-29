package com.example.moza.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.ChatMessage
import com.example.moza.models.NotificationRetrofitObj
import com.example.moza.models.PushNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatAttachmentViewModel @Inject constructor(val repository: FireBaseRepository) :
    ViewModel() {

    val sendUriMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendDocMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendVideoMessageResponse: MutableLiveData<Resource<String>> = MutableLiveData()


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

    fun safeSendDocument(chatID: String, uri: Uri, message: ChatMessage, userId: String,filename: String) =
        viewModelScope.launch {
            sendDocument(chatID, uri, message, userId, filename )
        }

    private suspend fun sendDocument(
        chatID: String,
        uri: Uri,
        message: ChatMessage,
        userId: String,
        filename: String
    ) {
        repository.sendDocMessage(chatID, message, uri, userId , filename).collect{ response ->
            when (response) {
                is Resource.Error -> sendDocMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Success -> sendDocMessageResponse.postValue(Resource.Success(response.data!!))
                is Resource.Loading -> sendDocMessageResponse.postValue(Resource.Loading())
            }
        }
    }

    fun safeSendVideo(chatID: String, uri: Uri, message: ChatMessage, userId: String,filename: String) =
        viewModelScope.launch {
            sendVideo(chatID, uri, message, userId, filename )
        }

    private suspend fun sendVideo(
        chatID: String,
        uri: Uri,
        message: ChatMessage,
        userId: String,
        filename: String
    ) {
        repository.sendVideoMessage(chatID, message, uri, userId , filename).collect{ response ->
            when (response) {
                is Resource.Error -> sendVideoMessageResponse.postValue(Resource.Error(response.message!!))
                is Resource.Success -> sendVideoMessageResponse.postValue(Resource.Success(response.data!!))
                is Resource.Loading -> sendVideoMessageResponse.postValue(Resource.Loading())
            }
        }
    }


    fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NotificationRetrofitObj.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d("notification", "Notification sent")
                } else {
                    Log.e("error in sending notification", response.errorBody().toString())
                }
            } catch (e: Exception) {
                Log.e("error in sending notification", e.toString())
            }
        }


}