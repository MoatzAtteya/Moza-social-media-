package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    val repository: FireBaseRepository
) : ViewModel() {

    var getUsersResponse: MutableLiveData<MutableList<User>> = MutableLiveData()
    var getSearchHistoryResponse: MutableLiveData<Resource<MutableList<User>>> = MutableLiveData()
    //var removeSearchHistoryResponse: MutableLiveData<Res> = MutableLiveData()

    fun getUserData(username: String) = viewModelScope.launch {
        try {
            val response = repository.getUsersDataByName(username)
            handleResponse(response)
        } catch (e: Exception) {
            Log.e("searching for user: ", e.message.toString())
        }
    }

    private fun handleResponse(response: Task<QuerySnapshot>) {
        response.addOnCompleteListener {
            if (it.isSuccessful) {
                var users = it.result.toObjects(User::class.java)
                getUsersResponse.postValue(users)
            } else {
                Log.e("searching for user: ", it.exception!!.message.toString())
            }
        }.addOnFailureListener {
            Log.e("searching for user: ", it.message.toString())
        }
    }

    fun getSearchHistory(userId: String) = viewModelScope.launch {
        repository.getSearchHistory2(userId).collect{ response->
            when(response){
                is Resource.Error -> getSearchHistoryResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success ->  getSearchHistoryResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }



    fun removeSearchItem(userId: String, id: String) = viewModelScope.launch {
        removeSearchItemResponse(userId, id)
    }

    private suspend fun removeSearchItemResponse(userId: String, id: String) {
        repository.deleteSearchHistoryItem(userId, id).collect {
            Log.d("Deleting search history item:", it.message.toString() + it.data.toString())
        }
    }

}
