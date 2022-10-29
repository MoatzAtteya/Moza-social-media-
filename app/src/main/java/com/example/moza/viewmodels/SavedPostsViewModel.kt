package com.example.moza.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.Constants
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.*
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SavedPostsViewModel @Inject constructor(var repository: FireBaseRepository , var firebaseUser: FirebaseUser) : ViewModel() {


    private var _getPostsResponse: MutableLiveData<Resource<MutableList<PostImage>>> =
        MutableLiveData()
    val getPostsResponse: LiveData<Resource<MutableList<PostImage>>> get() = _getPostsResponse

    private var _sendCommentResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendCommentResponse : LiveData<Resource<String>> get() = _sendCommentResponse

    private var _updateSavedPostsResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val updateSavedPostsResponse : LiveData<Resource<String>> get() = _updateSavedPostsResponse

    fun getUsersPosts(usersId: ArrayList<String>) = viewModelScope.launch {
        repository.getUsersPostsByID(usersId).collect { response ->
            when (response) {
                is Resource.Error -> _getPostsResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> _getPostsResponse.postValue(Resource.Loading())
                is Resource.Success -> {
                    _getPostsResponse.postValue(Resource.Success(response.data as MutableList<PostImage>))
                }
            }
        }
    }

    fun safeDeleteNotification(userId: String, id: String, postUrl: String) =
        viewModelScope.launch {
            repository.deleteUserNotification(userId, id, postUrl).collect {}
        }

    fun safeUpdateNotifications(userId: String, postUrl: String, postID: String) =
        viewModelScope.launch {
            repository.getUserData2(userId).collect {
                val user2 = it.data
                val token = user2!!.token
                val notification = Notification(
                    "",
                    user2.fullName,
                    postUrl,
                    user2.id,
                    Constants.NOTIFICATION_COMMENT_TYPE,
                    System.currentTimeMillis(),
                    postID
                )
                repository.getUserData2(notification.uid).collect {
                    val user = it.data
                    notification.username = user!!.fullName
                    updateNotifications(userId, notification, user, postUrl, token)
                }
            }
        }

    private suspend fun updateNotifications(
        userId: String,
        notification: Notification,
        user: User?,
        postUrl: String,
        token: String?
    ) {
        repository.updateUserNotifications(userId, notification).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating User Notifications: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    println("tokeeeeen:$token")
                    Log.d("Updating User Notifications: ", response.data!!)
                    val notify = NotificationsData(
                        user!!.fullName,
                        postUrl,
                        "${user.fullName} commented on your photo",
                        Random().nextInt(),
                        Constants.NOTIFICATION_COMMENT_TYPE
                    )
                    val pushNotification = PushNotification(notify, token!!)
                    sendNotification(pushNotification)
                }
            }
        }
    }

    fun updatePostLikesList(userId: String, postId: String, likesList: ArrayList<String>) = viewModelScope.launch {
        repository.updateUserLikesList(postId, likesList).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating post like list: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.e("Updating post like list: ", response.data!!)
            }
        }
    }

    fun updatePostComments(postId: String, comment: String) = viewModelScope.launch{
        repository.updatePostComments(postId, comment).collect{ response ->
            when (response) {
                is Resource.Error -> _sendCommentResponse.postValue(Resource.Error(response.data!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _sendCommentResponse.postValue(Resource.Success("Done"))

            }
        }
    }


    private fun sendNotification(notification: PushNotification) =
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

    fun safeUpdateNotifications2(userId: String, notification: Notification) =
        viewModelScope.launch {
            repository.getUserData2(userId).collect {
                val token = it.data!!.token
                repository.getUserData2(notification.uid).collect {
                    val user = it.data
                    notification.username = user!!.fullName
                    updateNotifications(userId, notification, user, token)
                }
            }
        }

    private suspend fun updateNotifications(
        userId: String,
        notification: Notification,
        user: User?,
        token: String?
    ) {
        repository.updateUserNotifications(userId, notification).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating User Notifications: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Log.d("Updating User Notifications: ", response.data!!)
                    val notify = NotificationsData(
                        user!!.fullName,
                        user.profilePicture,
                        "${user.fullName} liked your photo",
                        Random().nextInt(),
                        Constants.NOTIFICATION_LIKE_TYPE
                    )
                    val pushNotification = PushNotification(notify, token!!)
                    sendNotification(pushNotification)
                }
            }
        }
    }

    fun updateSavedPostsList(postId: String) = viewModelScope.launch {
        getUserData(firebaseUser.uid , postId)
    }

    private suspend fun getUserData(uid: String , postId: String) {
        repository.getUserData2(uid).collect{response->
            when(response){
                is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    val user = response.data
                    updateUSerSavedList(user!!.savedPosts , postId)
                }
            }
        }
    }

    private suspend fun updateUSerSavedList(savedPosts: ArrayList<String>, postId: String) {
        savedPosts.remove(postId)
        repository.updateUserSavedPosts(firebaseUser.uid , savedPosts).collect{response->
            when(response){
                is Resource.Error ->_updateSavedPostsResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _updateSavedPostsResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }
}