package com.example.moza.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowersViewModel @Inject constructor(val repository: FireBaseRepository ) : ViewModel(){


    var getUsersResponse: MutableLiveData<Resource<MutableList<User>>> = MutableLiveData()


    fun getUsers(usersIDS : ArrayList<String>) = viewModelScope.launch {
        repository.getUsers(usersIDS).collect{ response->
            when(response){
                is Resource.Error -> getUsersResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {

                }
                is Resource.Success ->  getUsersResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

}