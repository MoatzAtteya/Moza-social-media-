package com.example.moza.viewmodels

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.Constants
import com.example.moza.common.Constants.PREF_URL
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.*
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.UploadTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val sharedPreferences: SharedPreferences,
    val repository: FireBaseRepository,
    val contextWrapper: ContextWrapper
) :
    ViewModel() {

    var getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()
    var getUserFollowListResponse: MutableLiveData<ArrayList<String>> = MutableLiveData()
    var storeImageToStorageResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    var createChatResponse: MutableLiveData<Resource<String>> = MutableLiveData()

    private var _getPostsSize: MutableLiveData<Resource<Int>> = MutableLiveData()
    val getPostsSize: LiveData<Resource<Int>> get() = _getPostsSize


    fun getUserData(uID: String) = viewModelScope.launch {
        repository.getUserData2(uID).collect { response ->
            when (response) {
                is Resource.Error -> getUserDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    getUserDataResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }

    fun getUserFollowList(uID: String) {
        //var user = User()
        Log.d("user id : ", uID.toString())
        try {
            val document = repository.getUserData(uID)
            document.addOnSuccessListener {
                //user = it.toObject<User>()!!
                getUserFollowListResponse.postValue(it.get("following") as ArrayList<String>)
            }

        } catch (e: Exception) {

            Log.e("getting user data from firestore: ", e.message.toString())
            e.printStackTrace()
        }
    }

    fun getUserPostsImages(uID: String): FirestoreRecyclerOptions<PostImage> {
        val options = repository.getUserPostImagesRecyclerView(uID)
        return options
    }

    fun storeImageToStorage(
        userId: String,
        imageUri: Uri,
    ) {
        try {
            storeImageToStorageResponse.postValue(Resource.Loading())
            val response = repository.storeImageToStorage(imageUri)
            handleStoringImageResponse(userId, response, imageUri)

        } catch (e: Exception) {
            storeImageToStorageResponse.postValue(Resource.Error("Error: ${e.message.toString()}"))
            e.printStackTrace()
        }
    }

    fun uploadImageToFirestore(userId: String, imageUrl: String) {
        try {
            val response = repository.uploadProfileImage(userId, imageUrl)
            handleUploadImageToFSResponse(response)

        } catch (e: Exception) {
            storeImageToStorageResponse.postValue(Resource.Error("Error: ${e.message.toString()}"))
            e.printStackTrace()
        }
    }


    private fun handleStoringImageResponse(
        userId: String,
        response: UploadTask,
        uri: Uri
    ) {
        var storageReference =
            repository.storageReference
        response.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                storageReference.downloadUrl.addOnSuccessListener {
                    var request = UserProfileChangeRequest.Builder()
                    request.photoUri = uri
                    var user = FirebaseAuth.getInstance().currentUser
                    user!!.updateProfile(request.build())
                    uploadImageToFirestore(userId, it.toString())
                }.addOnFailureListener {
                    storeImageToStorageResponse.postValue(Resource.Error("Error: ${it.message.toString()}"))
                }
            } else {
                storeImageToStorageResponse.postValue(Resource.Error("Error while storing your image in the storage."))
            }
        }
    }

    private fun handleUploadImageToFSResponse(response: Task<Void>) {
        response.addOnCompleteListener {
            if (it.isSuccessful) {
                storeImageToStorageResponse.postValue(Resource.Success("Your profile has been updated."))
            } else {
                storeImageToStorageResponse.postValue(Resource.Error("Error while storing your image in the storage."))
            }
        }
    }

    fun updateUserFollowList(userId: String, list: ArrayList<String>, type: String) =
        viewModelScope.launch {
            safeUpdateFollowList(userId, list, type)
        }

    suspend fun safeUpdateFollowList(userId: String, list: ArrayList<String>, type: String) {
        try {
            val response = repository.updateUserFollowList(userId, list, type)
            handleUpdateFollowList(response)
        } catch (e: Exception) {
            Log.e("updating follow list: ", e.message.toString())
        }
    }

    private fun handleUpdateFollowList(response: Task<Void>) {
        if (response.isSuccessful) {
            response.addOnCompleteListener {
                Log.d("user follow list", "updated")
            }.addOnFailureListener {
                Log.d("user follow list", it.message.toString())
            }
        }
    }

    fun storeImageToDevice(bitmap: Bitmap, url: String, fullName: String) {

        var isStored = sharedPreferences.getBoolean(Constants.PREF_STORED, false)
        var stringUrl = sharedPreferences.getString(PREF_URL, "url")
        val prefEditor = sharedPreferences.edit()

        if (isStored && stringUrl.equals(url))
            return

        val directory = contextWrapper.getDir("image_data", Context.MODE_PRIVATE)

        if (!directory.exists())
            directory.mkdir()

        val path = File(directory, "profile.png")
        var outPutStream: FileOutputStream? = null

        try {
            outPutStream = FileOutputStream(path)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outPutStream)

        } catch (e: FileNotFoundException) {
            Log.e("storing profile img: ", e.message.toString())
            // e.printStackTrace()
        } finally {
            try {
                outPutStream?.close()
            } catch (e: IOException) {
                Log.e("storing profile img: ", e.message.toString())
                // e.printStackTrace()
            }
        }

        prefEditor.putBoolean(Constants.PREF_STORED, true)
        prefEditor.putString(Constants.PREF_URL, url)
        prefEditor.putString("USER_NAME", fullName)
        prefEditor.putString(Constants.PREF_DIRECTORY, directory.absolutePath)
        prefEditor.apply()

    }

    fun updateSearchHistory(userId: String, user: User) = viewModelScope.launch {
        try {
            val response = repository.updateSearchHistory(userId, user)
            handleUpdateSearchList(response)
        } catch (e: Exception) {
            Log.e("updating search history list: ", e.message.toString())
        }
    }

    private fun handleUpdateSearchList(response: Task<Void>) {
        response.addOnCompleteListener {
            if (!it.isSuccessful) {
                Log.e(
                    "Error while updating search history list: ",
                    it.exception!!.message.toString()
                )
            }
        }
    }

    fun safeUpdateUserStatus(userId: String) = viewModelScope.launch {
        updateUserStatus(userId)
    }

    private suspend fun updateUserStatus(userId: String) {
        repository.updateUserOnlineStatus(userId, false).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Error while changing user status: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Changing user status: ", response.data!!)
            }
        }
    }

    fun safeStartChat(chatUser: ChatUser, chatMessage: ChatMessage) = viewModelScope.launch {
        startChat(chatUser, chatMessage)
    }

    private suspend fun startChat(chatUser: ChatUser, chatMessage: ChatMessage) {
        repository.createChat(chatUser, chatMessage).collect { response ->
            when (response) {
                is Resource.Error -> createChatResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {

                }
                is Resource.Success -> createChatResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


    fun getPostsSize(userId: String) = viewModelScope.launch {
        repository.getUserPostsListSize(userId).collect { response ->
            when (response) {
                is Resource.Error -> _getPostsSize.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _getPostsSize.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun sendFollowNotification(followingUserID: String, fullName: String, token: String) = viewModelScope.launch {
        repository.getUserData2(followingUserID).collect { response ->
            when (response) {
                is Resource.Error -> getUserDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    val user = response.data!!
                    val notification = NotificationsData(
                        fullName,
                        user.profilePicture,
                        "${user.fullName} started following you.",
                        0,
                        Constants.NOTIFICATION_MESSAGE_TYPE
                    )
                    val pushNotification = PushNotification(notification, token)
                    sendNotification(pushNotification)
                }
            }
        }
    }

    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("notification before send: $notification")
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


