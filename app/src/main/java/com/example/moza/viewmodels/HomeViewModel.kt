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
class HomeViewModel @Inject constructor(
    val repository: FireBaseRepository,
    val firebaseUser: FirebaseUser
) : ViewModel() {


    var sendCommentResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    var getPostsResponse2: MutableLiveData<Resource<MutableList<PostImage>>> = MutableLiveData()
    var getUserFollowListResponse: MutableLiveData<ArrayList<String>> = MutableLiveData()
    var getUsersStoriesResponse: MutableLiveData<Resource<MutableList<Story>>> = MutableLiveData()

    private var _hidePostResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val hidePostResponse: LiveData<Resource<String>> get() = _hidePostResponse

    private var _savePostResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val savePostResponse: LiveData<Resource<String>> get() = _savePostResponse

    private var _getUserDataResponse: MutableLiveData<Resource<User>> = MutableLiveData()
     val getUserDataResponse: LiveData<Resource<User>> get() = _getUserDataResponse



    fun updatePostLikesList(userId: String, postId: String, likesList: ArrayList<String>) =
        viewModelScope.launch {
            repository.updateUserLikesList(postId, likesList).collect { response ->
                when (response) {
                    is Resource.Error -> Log.e("Updating post like list: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> Log.e("Updating post like list: ", response.data!!)
                }
            }
        }



    fun sendComment(postID: String , comment : String) = viewModelScope.launch {
        repository.sendComment(postID , comment).collect{ response->
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

    fun getUserData() = viewModelScope.launch {
        repository.getUserData2(firebaseUser.uid).collect{response->
            when(response){
                is Resource.Error -> _getUserDataResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _getUserDataResponse.postValue(Resource.Success(response.data!!))
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
                //getUserDataResponse.postValue(it.toObject(User::class.java))
                getUserFollowListResponse.postValue(it.get("following") as ArrayList<String>)
            }
        } catch (e: Exception) {
            Log.e("getting user data from firestore: ", e.message.toString())
            e.printStackTrace()
        }
    }

    fun safeGetUsersPosts(idList: ArrayList<String>) = viewModelScope.launch {
        getUsersPosts(idList)

    }

    private suspend fun getUsersPosts(usersId: ArrayList<String>) {

        repository.getUsersPosts(usersId).collect() { response ->
            when (response) {
                is Resource.Error -> TODO()
                is Resource.Loading -> getPostsResponse2.postValue(Resource.Loading())
                is Resource.Success -> {
                    getPostsResponse2.postValue(Resource.Success(response.data as MutableList<PostImage>))
                }
            }
        }
    }

    fun safeGetUsersStories(idList: ArrayList<String>) = viewModelScope.launch {
        getUsersStories(idList)
    }

    private suspend fun getUsersStories(idList: java.util.ArrayList<String>) {
        repository.getUsersStories(idList).collect { response ->
            when (response) {
                is Resource.Error -> {
                    getUsersStoriesResponse.postValue(Resource.Error(response.message.toString()))
                }
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    getUsersStoriesResponse.postValue(Resource.Success(response.data!!))
                }
            }
        }
    }

    fun safeUpdateUserStatus(userId: String) = viewModelScope.launch {
        updateUserStatus(userId)
    }

    private suspend fun updateUserStatus(userId: String) {
        repository.updateUserOnlineStatus(userId, true).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Error while changing user status: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Changing user status: ", response.data!!)
            }
        }
    }

    fun safeGetAndStoreUserToken(userId: String) = viewModelScope.launch {
        getAndStoreUserToken(userId)
    }

    private suspend fun getAndStoreUserToken(userId: String) {
        repository.getUserFCMToken().collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting FCM Token: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    val token = response.data
                    safeStoreUserToken(userId, token!!)
                }
            }

        }
    }

    private suspend fun safeStoreUserToken(userId: String, token: String) {
        repository.storeFCMToken(userId, token).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Storing FCM Token: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> Log.d("Storing FCM Token: ", response.data!!)
            }
        }
    }

    fun safeUpdateNotifications(userId: String, notification: Notification) =
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

    fun safeDeleteNotification(userId: String, id: String, postUrl: String) =
        viewModelScope.launch {
            deleteNotification(userId, id, postUrl)
        }

    private suspend fun deleteNotification(userId: String, id: String, postUrl: String) {
        repository.deleteUserNotification(userId, id, postUrl).collect {

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

    fun updatePostHideIds(postId: String, hideIds: List<String>) = viewModelScope.launch {
        repository.updatePostHideIds(postId, hideIds).collect { response ->
            when (response) {
                is Resource.Error -> _hidePostResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _hidePostResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun updateUserSavedPosts(postId: String) = viewModelScope.launch {
        repository.getUserData2(firebaseUser.uid).collect { response ->
            when (response) {
                is Resource.Error -> _savePostResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val user = response.data
                    user!!.savedPosts.add(postId)
                    var newList = user.savedPosts.distinct()
                    updateUserSaved(firebaseUser.uid, newList)
                }
            }
        }
    }

    private suspend fun updateUserSaved(uid: String, postId: List<String>) {
        repository.updateUserSavedPosts(uid, postId).collect { response ->
            when (response) {
                is Resource.Error -> _savePostResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success -> _savePostResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}

