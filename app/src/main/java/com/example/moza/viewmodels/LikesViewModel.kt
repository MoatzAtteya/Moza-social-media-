package com.example.moza.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.Notification
import com.example.moza.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikesViewModel @Inject constructor(
    val repository : FireBaseRepository
) : ViewModel() {


    val userNotificationResponse: MutableLiveData<Resource<MutableList<Notification>>> =
        MutableLiveData()

    fun safeGetUserNotifications(userId: String) = viewModelScope.launch {
        userNotification(userId)
    }

    private suspend fun userNotification(userId: String) {
        repository.getUserNotifications(userId).collect { response ->
            when (response) {
                is Resource.Error -> userNotificationResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> userNotificationResponse.postValue(Resource.Loading())
                is Resource.Success -> userNotificationResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}

