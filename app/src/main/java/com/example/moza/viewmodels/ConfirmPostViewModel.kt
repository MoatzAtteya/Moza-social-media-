package com.example.moza.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.PostImage
import com.example.moza.common.Resource
import com.google.firebase.storage.UploadTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmPostViewModel @Inject constructor(
    val repository: FireBaseRepository
) : ViewModel() {

    var storeImageToStorageResponse: MutableLiveData<Resource<String>> = MutableLiveData()


    fun storeImageToStorage(
        description: String,
        imageUri: Uri,
        postImage: PostImage,
    ) {
        try {
            storeImageToStorageResponse.postValue(Resource.Loading())
            val response = repository.storeImageToStorage(imageUri)
            handleStoringImageResponse( response, description, postImage)

        } catch (e: Exception) {
            storeImageToStorageResponse.postValue(Resource.Error("Error: ${e.message.toString()}"))
            e.printStackTrace()
        }
    }


    private fun handleStoringImageResponse(
        response: UploadTask,
        description: String,
        postImage: PostImage
    ) {
        var storageReference =
            repository.storageReference

        response.addOnCompleteListener { task ->
            if (task.isSuccessful) {

                storageReference.downloadUrl.addOnSuccessListener {
                    println("image url is: ${it.toString()}")

                    //uploadImageToFirestore(userId, it.toString(), description, postImage)
                    safeUploadPost( it.toString(), description, postImage)
                }.addOnFailureListener {
                    storeImageToStorageResponse.postValue(Resource.Error("Error: ${it.message.toString()}"))
                }
            } else {
                storeImageToStorageResponse.postValue(Resource.Error("Error while storing your image in the storage."))
            }
        }
    }

    fun safeUploadPost(
        imageUrl: String,
        description: String,
        postImage: PostImage
    ) = viewModelScope.launch {
        uploadPost( imageUrl , description , postImage)
    }

    private suspend fun uploadPost(
        imageUrl: String,
        description: String,
        postImage: PostImage
    ) {
        repository.uploadPost( imageUrl, description, postImage).collect{ response->
            when(response){
                is Resource.Error -> storeImageToStorageResponse.postValue(Resource.Error("Error while storing your image in the storage."))

                is Resource.Loading -> {}
                is Resource.Success -> storeImageToStorageResponse.postValue(Resource.Success("Your post has been uploaded."))
            }

        }
    }


 /*   private fun handleUploadImageToFSResponse(response: Task<Void>) {
        response.addOnCompleteListener {
            if (it.isSuccessful) {
                storeImageToStorageResponse.postValue(Resource.Success("Your post has been uploaded."))
            } else {
                storeImageToStorageResponse.postValue(Resource.Error("Error while storing your image in the storage."))
            }
        }
    }*/

}

