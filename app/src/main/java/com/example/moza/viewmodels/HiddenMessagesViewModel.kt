package com.example.moza.viewmodels

import android.util.Log
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
class HiddenMessagesViewModel @Inject constructor(val fireBaseRepository: FireBaseRepository) : ViewModel() {


    private val _getHiddenMessages : MutableLiveData<Resource<MutableList<ChatUser>>> = MutableLiveData()
    val getHiddenMessages : LiveData<Resource<MutableList<ChatUser>>> get() = _getHiddenMessages



    fun safeGetHiddenMessages(userID: String) = viewModelScope.launch {
        getHiddenMessages(userID)
    }

    private suspend fun getHiddenMessages(userID: String) {
        fireBaseRepository.getHiddenMessage(userID).collect{ response->
            when(response){
                is Resource.Error -> _getHiddenMessages.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _getHiddenMessages.postValue(Resource.Success(response.data!!))
            }
        }

    }

    fun safeShowChat(chatId : String , hideIDS: ArrayList<String>) = viewModelScope.launch {
        showChat(chatId , hideIDS)
    }

    private suspend fun showChat(chatId: String, hideIDS: ArrayList<String>) {
        fireBaseRepository.showChat(chatId , hideIDS).collect{ response->
            when(response){
                is Resource.Error -> Log.e("Showing chat: " , response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Showing chat: " , response.data!!)
            }
        }
    }

}