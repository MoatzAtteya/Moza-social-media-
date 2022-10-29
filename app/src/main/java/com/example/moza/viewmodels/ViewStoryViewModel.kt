package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.StoryView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewStoryViewModel @Inject constructor(val repository: FireBaseRepository) : ViewModel() {

    val deleteStoryResponse = MutableStateFlow<String>("")

    fun safeUploadView(storyID : String , view: StoryView) = viewModelScope.launch {
        uploadView(storyID , view)
    }

    private suspend fun uploadView(storyID: String, view: StoryView) {
        repository.uploadStoryView(storyID, view).collect{ response->
            when(response){
                is Resource.Error -> Log.e("Uploading story view: " , response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Uploading story view: " , response.data!!)
            }
        }
    }

    fun deleteStory(storyID: String) = viewModelScope.launch {
        repository.deleteStory(storyID).collect{response->
            when(response){
                is Resource.Error -> deleteStoryResponse.value = "Error"
                is Resource.Loading -> TODO()
                is Resource.Success -> deleteStoryResponse.value = "Story has been deleted"
            }
        }
    }
}
