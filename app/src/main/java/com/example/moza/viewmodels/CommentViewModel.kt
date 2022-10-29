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
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    val repository: FireBaseRepository
) : ViewModel() {

    var sendCommentResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    var getCommentsResponse: MutableLiveData<Resource<ArrayList<Comment>>> = MutableLiveData()

    private var _deleteCommentResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteCommentResponse : LiveData<Resource<String>> get() = _deleteCommentResponse

    fun updatePostComments(postId: String, comment: String) = viewModelScope.launch{
        repository.updatePostComments(postId, comment).collect{ response ->
            when (response) {
                is Resource.Error -> sendCommentResponse.postValue(Resource.Error(response.data!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> sendCommentResponse.postValue(Resource.Success("Done"))

            }
        }
    }

    fun getPostComments(postUID: String, postId: String) {
        try {
            getCommentsResponse.postValue(Resource.Loading())
            val response = repository.getPostComments(postUID, postId)
            handleGetPostCommentsResponse(response)
        } catch (e: Exception) {
            Log.e("Error while getting post comments: ", e.message.toString())
            e.printStackTrace()
        }
    }

    private fun handleGetPostCommentsResponse(response: Query) {
        response.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e("getting post comments: ", error.message.toString())
                getCommentsResponse.postValue(Resource.Error(error.message.toString()))
                return@addSnapshotListener
            }
            if (value == null) {
                getCommentsResponse.postValue(Resource.Error("Post Comment list is empty!"))
                return@addSnapshotListener
            }
            println("comment list: ${value.toObjects(Comment::class.java)}")
            val commentList = value.toObjects(Comment::class.java) as ArrayList<Comment>
            getCommentsResponse.postValue(Resource.Success(commentList))

        }
    }

    fun safeUpdateNotifications(userId: String, senderID: String,postUrl : String, postId: String) =
        viewModelScope.launch {
            repository.getUserData2(senderID).collect {
                val user2 = it.data
                val token =user2!!.token
                val notification = Notification(
                    "",
                    user2!!.fullName,
                    postUrl,
                    user2.id,
                    Constants.NOTIFICATION_COMMENT_TYPE,
                    System.currentTimeMillis(),
                    postId
                )
                repository.getUserData2(notification.uid).collect {
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
        println("token to send: $token")
        repository.updateUserNotifications(userId, notification).collect { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating User Notifications: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
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

    fun deleteComment(postId: String , commentId : String) = viewModelScope.launch {
        repository.deleteComment(postId,commentId).collect{response->
            when(response){
                is Resource.Error -> _deleteCommentResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> _deleteCommentResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

}

