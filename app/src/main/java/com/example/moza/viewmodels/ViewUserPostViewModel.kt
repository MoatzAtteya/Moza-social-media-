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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ViewUserPostViewModel : ViewModel() {
    val fireBaseRepository = FireBaseRepository()

    var userPostResponse: MutableLiveData<Resource<PostImage>> = MutableLiveData()
    var sendCommentResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    var getPostComments: MutableLiveData<Resource<MutableList<Comment>>> = MutableLiveData()

    private var _deletePostResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteResponse : LiveData<Resource<String>> get() =  _deletePostResponse

    fun safeGetPost( postID: String) = viewModelScope.launch {
        getUserPost( postID)
    }

    private suspend fun getUserPost( postID: String) {
        fireBaseRepository.getUserPost( postID).collect { response ->
            when (response) {
                is Resource.Error -> userPostResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> userPostResponse.postValue(Resource.Loading())
                is Resource.Success -> userPostResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun updatePostLike(postImage: PostImage) = viewModelScope.launch {
        fireBaseRepository.updatePostsLike(postImage).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating like list: ", response.message.toString())
                is Resource.Loading -> {}
                is Resource.Success -> Log.d("Updating like list: ", response.data.toString())
            }
        }
    }

    fun safeUpdateNotifications(userId: String, notification: Notification) =
        viewModelScope.launch {
            fireBaseRepository.getUserData2(userId).collect {
                val token = it.data!!.token
                fireBaseRepository.getUserData2(notification.uid).collect {
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
        fireBaseRepository.updateUserNotifications(userId, notification).collect { response ->
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

    fun safeDeleteNotification(userId: String , id:  String , postUrl : String) = viewModelScope.launch {
        fireBaseRepository.deleteUserNotification(userId , id, postUrl).collect { response->
            when (response) {
                is Resource.Error -> Log.e("Deleting like notification: ", response.message.toString())
                is Resource.Loading -> {}
                is Resource.Success -> Log.d("Deleting like notification: ", response.data.toString())
            }
            }
        }

    fun sendComment(postImage: PostImage , comment : String) = viewModelScope.launch {
        fireBaseRepository.sendComment(postImage.id!! , comment).collect{ response->
            when (response) {
                is Resource.Error -> {
                    Log.e("Sending comment: ", response.message.toString())
                    sendCommentResponse.postValue(Resource.Error(response.message!!))

                }
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Log.e("Sending comment: ", response.data.toString())
                    sendCommentResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }


    fun safeUpdateNotifications(userId: String,postUrl : String) =
        viewModelScope.launch {
            fireBaseRepository.getUserData2(userId).collect {
                val user2 = it.data
                val token =user2!!.token
                val notification = Notification(
                    "",
                    user2!!.fullName,
                    postUrl,
                    user2.id,
                    Constants.NOTIFICATION_COMMENT_TYPE,
                    System.currentTimeMillis()
                )
                fireBaseRepository.getUserData2(notification.uid).collect {
                    val user = it.data
                    notification.username = user!!.fullName
                    updateNotifications(userId, notification, user , postUrl , token)
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
        fireBaseRepository.updateUserNotifications(userId, notification).collect { response ->
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

    fun safeGetComments(userId: String, postId: String) = viewModelScope.launch {
        getComments(userId , postId)
    }

    private suspend fun getComments(userId: String, postId: String) {
        fireBaseRepository.getPostComments2(userId, postId).collect{response->
            when(response){
                is Resource.Error -> getPostComments.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> getPostComments.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun deletePost(postID: String) = viewModelScope.launch {
        fireBaseRepository.deletePost(postID).collect{response->
            when(response){
                is Resource.Error -> _deletePostResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _deletePostResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}