package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageReactViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel() {

    fun safeSendReact(chatID: String , messageID : String , react : String) = viewModelScope.launch {
        sendReact(chatID , messageID , react)
    }

    private suspend fun sendReact(chatID: String, messageID: String, react: String) {
        repository.updateMessageReact(chatID, messageID, react).collect{response->
            when(response){
                is Resource.Error -> Log.e("Updating message react: " , response.message!!)
                is Resource.Loading -> return@collect
                is Resource.Success -> Log.d("Updating message react: " , response.data!!)
            }
        }
    }

}