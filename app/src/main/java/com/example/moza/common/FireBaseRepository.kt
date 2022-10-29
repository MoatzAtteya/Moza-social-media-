package com.example.moza.common

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.moza.models.*
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


class FireBaseRepository {

    var firebaseStorage = FirebaseStorage.getInstance()
    var storageReference: StorageReference =
        firebaseStorage.getReference().child("Post Images/${System.currentTimeMillis()}")

    private val reference =
        FirebaseFirestore.getInstance().collection("Users")
    private val msgReference = FirebaseFirestore.getInstance().collection("Messages")

    var lastResult: DocumentSnapshot? = null

    fun registerUserFirebase(user: User, password: String) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        FirebaseFirestore.getInstance().collection("Users")
                            .document(firebaseUser.uid).set(user)
                            .addOnCompleteListener { voidTask ->
                                if (voidTask.isSuccessful) {
                                    trySend(Resource.Success("user is stored successfully."))
                                    Log.d(
                                        "Storing user in firestore: ",
                                        "user is stored successfully."
                                    )
                                } else {
                                    trySend(Resource.Error(voidTask.exception!!.message.toString()))
                                }
                            }.addOnFailureListener { exception ->
                                trySend(Resource.Error(exception.message.toString()))
                            }
                        firebaseUser.sendEmailVerification()
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val request = UserProfileChangeRequest.Builder()
                                    request.displayName = user.fullName
                                    firebaseUser.updateProfile(request.build())
                                    // handleStoreUserResponse(storeUserResponse)
                                }
                            }
                    } else {
                        trySend(Resource.Error(it.exception!!.message.toString()))
                    }
                }
            }.addOnFailureListener {
                trySend(Resource.Error(it.message.toString()))
            }
        awaitClose { }
    }


    fun uploadUserData2(user: User, context: Context) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        FirebaseFirestore
            .getInstance()
            .collection("Users").document(user.id).set(user)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("User data uploaded."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }

        awaitClose()
    }

    fun userLogin(email: String, pass: String) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("User logged in successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun googleLogIn(idToken: String) = callbackFlow<Resource<FirebaseUser>> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val auth = FirebaseAuth.getInstance()
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                trySend(Resource.Success(it.result.user!!))
            } else {
                trySend(Resource.Error(it.exception!!.message!!))
            }
        }
        awaitClose()
    }

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        val docRef = FirebaseFirestore.getInstance().collection("Users").document(userId)
        val documentSnapshot = docRef.get()
        return documentSnapshot

    }

    fun getUserData2(userId: String) = callbackFlow {
        val listener = FirebaseFirestore.getInstance().collection("Users").document(userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Chat is empty"))
                    return@addSnapshotListener
                }
                trySend(Resource.Success(value.toObject(User::class.java)!!))

            }
        awaitClose { listener.remove() }
    }

    fun getUserDataLive(userId: String): DocumentReference {
        return FirebaseFirestore.getInstance().collection("Users").document(userId)

    }

    suspend fun getUsersDataByName(username: String): Task<QuerySnapshot> {

        return FirebaseFirestore.getInstance().collection("Users")
            .orderBy("fullName").startAt(username).endAt(username + "\uf8ff").get()

    }

    fun getUserPostImagesRecyclerView(userId: String): FirestoreRecyclerOptions<PostImage> {
        val query =
            FirebaseFirestore.getInstance().collection("Post Images").whereEqualTo("uid", userId)

        return FirestoreRecyclerOptions.Builder<PostImage>()
            .setQuery(query, PostImage::class.java)
            .build()
    }

    fun getUserPostsListSize(userId: String) = callbackFlow<Resource<Int>> {
        val listener =
            FirebaseFirestore.getInstance().collection("Post Images").whereEqualTo("uid", userId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message.toString()))
                        return@addSnapshotListener
                    }
                    if (value == null) {
                        trySend(Resource.Error("Posts list is empty"))
                        return@addSnapshotListener
                    }
                    trySend(Resource.Success(value.toObjects(PostImage::class.java).size))
                }
        awaitClose {
            listener.remove()
        }
    }


    fun storeImageToStorage(imageUri: Uri): UploadTask {
        return storageReference.putFile(imageUri)
    }


    fun uploadPost(
        imageUrl: String,
        description: String,
        postImage: PostImage
    ) = callbackFlow<Resource<String>> {
        val reference =
            FirebaseFirestore.getInstance().collection("Post Images")
        val imageId = reference.document().id

        postImage.id = imageId
        postImage.imageUrl = imageUrl
        postImage.description = description
        reference.document(imageId).set(postImage).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("Post uploaded successfully."))
            else
                trySend(Resource.Error(it.exception!!.message.toString()))

        }.addOnFailureListener {
            trySend(Resource.Error("Check your internet connection."))
        }

        awaitClose()
    }

    fun uploadProfileImage(userId: String, imageUrl: String): Task<Void> {

        val response = reference.document(userId)
            .update("profilePicture", imageUrl)
        return response

    }

    suspend fun updateUserFollowList(
        userId: String,
        list: ArrayList<String>,
        type: String
    ): Task<Void> {
        return reference.document(userId)
            .update(type, list)
    }

    fun updateUserLikesList(
        postId: String,
        likesList: ArrayList<String>
    ) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .update("likes", likesList).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Likes list updated"))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun updatePostComments(
        postId: String,
        commentText: String
    ) = callbackFlow<Resource<String>> {
        var docRef = FirebaseFirestore.getInstance()
            .collection("Post Images")
            .document(postId)
            .collection("Comments")

        val commentId = docRef.document().id
        val comment = Comment(
            FirebaseAuth.getInstance().uid,
            postId,
            commentId,
            commentText,
            System.currentTimeMillis()
        )
        docRef.document(commentId).set(comment).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("comment list updated"))
            else
                trySend(Resource.Error(it.exception!!.message.toString()))
        }
        awaitClose()
    }

    fun deleteComment(postId: String, commentID: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance()
            .collection("Post Images")
            .document(postId)
            .collection("Comments").document(commentID).delete()
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Comment deleted successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }

        awaitClose()
    }


    fun sendComment(postId: String, comment: String) = callbackFlow<Resource<String>> {
        val docRef = FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .collection("Comments")
        val comment = Comment(
            FirebaseAuth.getInstance().currentUser!!.uid,
            postId,
            docRef.document().id,
            comment,
            System.currentTimeMillis()
        )
        docRef.document(comment.commentID!!).set(comment).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("Comment has been published successfully."))
            else
                trySend(Resource.Error(it.exception!!.message.toString()))
        }
        awaitClose()
    }

    fun getPostComments(userId: String, postId: String): Query {
        return FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .collection("Comments").orderBy("timeStamp")
    }

    fun getPostComments2(userId: String, postId: String) =
        callbackFlow<Resource<MutableList<Comment>>> {
            val listener =
                FirebaseFirestore.getInstance().collection("Post Images").document(postId)
                    .collection("Comments").orderBy("timeStamp")
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            trySend(Resource.Error(error.message.toString()))
                            return@addSnapshotListener
                        }
                        if (value == null) {
                            trySend(Resource.Error("Comment list is empty"))
                            return@addSnapshotListener
                        }
                        if (!value.isEmpty)
                            trySend(Resource.Success(value.toObjects(Comment::class.java)))
                    }
            awaitClose {
                listener.remove()
            }
        }

    fun updateSearchHistory(
        id: String,
        user: User
    ): Task<Void> {
        val searchUser = SearchUser(user.fullName, user.id, user.profilePicture)
        return reference.document(id)
            .collection("Search History").document(user.id).set(searchUser)
    }

    fun getSearchHistory2(userId: String) = callbackFlow<Resource<MutableList<User>>> {
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .collection("Search History")
            .get().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success(it.result.toObjects(User::class.java)))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }
        awaitClose()
    }


    fun getUsers(usersIDS: ArrayList<String>) = callbackFlow<Resource<MutableList<User>>> {
        trySend(Resource.Loading())
        if (usersIDS.size == 0) {
            trySend(Resource.Error("Following List is empty."))
            awaitClose()
            return@callbackFlow
        }
        FirebaseFirestore.getInstance().collection("Users").whereIn("id", usersIDS)
            .get().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success(it.result.toObjects(User::class.java)))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }
        awaitClose()
    }

    fun deleteSearchHistoryItem(userId: String, id: String) = callbackFlow<Resource<String>> {
        reference.document(userId).collection("Search History").document(id).delete()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    trySend(Resource.Success("deleted"))
                } else {
                    trySend(Resource.Error("deleted"))
                }
            }
        awaitClose { }
    }

    fun getUsersStories(idList: ArrayList<String>) = callbackFlow<Resource<MutableList<Story>>> {
        val list = idList.distinct()
        println("stories users: $list")
        val listener = FirebaseFirestore.getInstance().collection("Stories").whereIn("uid", list)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Story list is empty"))
                    return@addSnapshotListener
                }
                if (!value.isEmpty)
                    trySend(Resource.Success(value.toObjects(Story::class.java)))
            }
        awaitClose {
            listener.remove()
        }
    }


    fun uploadUserStory(uri: Uri, story: Story, filename: String) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        val storageRef = FirebaseStorage.getInstance().reference
            .child("Stories/$filename")
        storageRef.putFile(uri).addOnCompleteListener {
            if (it.isSuccessful) {
                it.result.storage.downloadUrl.addOnSuccessListener { uri1 ->
                    val docRef = FirebaseFirestore.getInstance().collection("Stories").document()
                    story.id = docRef.id
                    story.videoUrl = uri1.toString()
                    docRef.set(story).addOnCompleteListener {
                        trySend(Resource.Success("Your story has been uploaded."))
                    }
                }
            } else {
                trySend(Resource.Error(it.exception!!.message.toString()))
            }
        }
        awaitClose { }
    }


    fun getUserAllChats(userId: String) = callbackFlow<Resource<MutableList<ChatUser>>> {
        val listener = FirebaseFirestore.getInstance().collection("Messages")
            .whereArrayContains("uid", userId).addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Chat list is null"))
                    return@addSnapshotListener
                }
                Log.d("user chats is: ", value.toObjects(ChatUser::class.java).toString())
                trySend(Resource.Success(value.toObjects(ChatUser::class.java)))

            }
        awaitClose {
            listener.remove()
        }
    }

    fun updateUserOnlineStatus(userId: String, boolean: Boolean) = callbackFlow<Resource<String>> {
        reference.document(userId).update("online", boolean).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("user status changed"))
            else
                trySend(Resource.Success(it.exception!!.message.toString()))

        }
        awaitClose {}
    }

    fun getChatMessages(chatID: String) = callbackFlow<Resource<MutableList<ChatMessage>>> {
        val docRef = msgReference.document(chatID).collection("Messages")
        val listener = docRef.orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("chat is empty"))
                    return@addSnapshotListener
                }
                if (!value.isEmpty) {
                    trySend(Resource.Success(value.toObjects(ChatMessage::class.java)))
                }
            }

        awaitClose { listener.remove() }
    }

    fun createChat(chatUser: ChatUser, message: ChatMessage) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            chatUser.uid.reverse()
            val query = FirebaseFirestore.getInstance().collection("Messages")
                .whereEqualTo("uid", chatUser.uid)
            query.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result.toObjects(ChatUser::class.java)
                    if (snapshot.isNotEmpty()) {
                        val exitChat = snapshot[0]
                        val chatID = exitChat.id
                        exitChat.timeStamp = message.time
                        exitChat.lastMessage = message.message
                        msgReference.document(chatID!!).set(exitChat, SetOptions.merge())
                            .addOnCompleteListener {
                                val chatRef =
                                    msgReference
                                        .document(chatID!!).collection("Messages")
                                message.id = chatRef.document().id
                                chatRef.document(message.id!!).set(message)
                                    .addOnCompleteListener { task1 ->
                                        if (task1.isSuccessful) {
                                            trySend(Resource.Success(chatID))
                                        } else
                                            trySend(Resource.Error(task1.exception!!.message.toString()))
                                    }
                            }
                    } else {
                        chatUser.timeStamp = message.time
                        chatUser.id = msgReference.document().id
                        msgReference.document(chatUser.id!!).set(chatUser, SetOptions.merge())
                            .addOnCompleteListener {
                                val chatRef =
                                    msgReference.document(chatUser.id!!).collection("Messages")
                                message.id = chatRef.document().id
                                chatRef.document(message.id!!).set(message).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        trySend(Resource.Success(chatUser.id!!))
                                    } else
                                        trySend(Resource.Error(it.exception!!.message.toString()))

                                }
                            }
                    }
                }
            }
            awaitClose { }
        }

    fun createGroupChat(chatUser: ChatUser) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            chatUser.id = msgReference.document().id
            msgReference.document(chatUser.id!!).set(chatUser, SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        trySend(Resource.Success(chatUser.id!!))
                    else
                        trySend(Resource.Error(it.exception!!.message.toString()))

                }
            awaitClose {}
        }


    fun sendVoiceMessage(chatID: String, message: ChatMessage, uri: Uri, filename: String) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            val storageRef = FirebaseStorage.getInstance().reference
                .child("VoiceMessages/$filename")
            storageRef.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.storage.downloadUrl.addOnSuccessListener {
                        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                            .update("lastMessage", "Voice Message")

                        val docRef =
                            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                                .collection("Messages").document()
                        message.id = docRef.id
                        message.messageUrl = it.toString()
                        docRef.set(message).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Your message has been sent."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))

                        }
                    }
                } else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }.addOnFailureListener {
                trySend(Resource.Error("Check your internet connection."))
            }
            awaitClose()
        }

    fun sendUriMessage(
        chatID: String,
        message: ChatMessage,
        uri: Uri,
        filename: String,
        userId: String
    ) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            val storageRef = FirebaseStorage.getInstance().reference
                .child("Messages/$filename")
            storageRef.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.storage.downloadUrl.addOnSuccessListener {
                        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                            .get().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val chat = it.result.toObject(ChatUser::class.java)
                                    chat!!.unReadCounter = chat.unReadCounter + 1
                                    chat!!.unReadSender = userId
                                    chat!!.lastMessage = "photo"
                                    chat.timeStamp = message.time
                                    FirebaseFirestore.getInstance().collection("Messages")
                                        .document(chatID)
                                        .set(chat, SetOptions.merge())
                                }
                            }

                        val docRef =
                            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                                .collection("Messages").document()
                        message.id = docRef.id
                        message.messageUrl = it.toString()
                        message.messageType = 1
                        docRef.set(message).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Your message has been sent."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))

                        }
                    }
                } else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }.addOnFailureListener {
                trySend(Resource.Error("Check your internet connection."))
            }
            awaitClose()
        }

    fun sendDocMessage(
        chatID: String,
        message: ChatMessage,
        uri: Uri,
        userId: String,
        filename: String
    ) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            val storageRef = FirebaseStorage.getInstance().reference
                .child("Messages/$filename")
            storageRef.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.storage.downloadUrl.addOnSuccessListener {
                        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                            .get().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val chat = it.result.toObject(ChatUser::class.java)
                                    chat!!.unReadCounter = chat.unReadCounter + 1
                                    chat!!.unReadSender = userId
                                    chat!!.lastMessage = "Document"
                                    chat.timeStamp = message.time
                                    FirebaseFirestore.getInstance().collection("Messages")
                                        .document(chatID)
                                        .set(chat, SetOptions.merge())
                                }
                            }

                        val docRef =
                            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                                .collection("Messages").document()
                        message.id = docRef.id
                        message.messageUrl = it.toString()
                        message.messageType = 4
                        docRef.set(message).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Your message has been sent."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))

                        }
                    }
                } else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }.addOnFailureListener {
                trySend(Resource.Error("Check your internet connection."))
            }
            awaitClose()
        }

    fun sendVideoMessage(
        chatID: String,
        message: ChatMessage,
        uri: Uri,
        userId: String,
        filename: String
    ) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            val storageRef = FirebaseStorage.getInstance().reference
                .child("Messages/$filename")
            storageRef.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.storage.downloadUrl.addOnSuccessListener {
                        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                            .get().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val chat = it.result.toObject(ChatUser::class.java)
                                    chat!!.unReadCounter = chat.unReadCounter + 1
                                    chat!!.unReadSender = userId
                                    chat!!.lastMessage = "Video"
                                    chat.timeStamp = message.time
                                    FirebaseFirestore.getInstance().collection("Messages")
                                        .document(chatID)
                                        .set(chat, SetOptions.merge())
                                }
                            }

                        val docRef =
                            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                                .collection("Messages").document()
                        message.id = docRef.id
                        message.messageUrl = it.toString()
                        message.messageType = 5
                        docRef.set(message).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Your message has been sent."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))

                        }
                    }
                } else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }.addOnFailureListener {
                trySend(Resource.Error("Check your internet connection."))
            }
            awaitClose()
        }

    fun sendMessage(chatID: String, message: ChatMessage, userId: String) =
        callbackFlow<Resource<String>> {
            FirebaseFirestore.getInstance().collection("Messages").document(chatID).get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val chat = it.result.toObject(ChatUser::class.java)
                        chat!!.unReadCounter = chat.unReadCounter + 1
                        chat.unReadSender = userId
                        chat.lastMessage = message.message
                        chat.timeStamp = message.time
                        message.id =
                            msgReference.document(chatID).collection("Messages")
                                .document().id
                        chat.lastMessage_id = message.id!!

                        msgReference.document(chatID).collection("Messages")
                            .document(message.id!!)
                            .set(message).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    FirebaseFirestore.getInstance().collection("Messages")
                                        .document(chatID)
                                        .set(chat, SetOptions.merge()).addOnCompleteListener {
                                            if (it.isSuccessful) {
                                                trySend(Resource.Success("Message is sent"))
                                            }
                                        }
                                } else
                                    trySend(Resource.Error("Message not sent: ${it.exception!!.message.toString()}"))
                            }


                    } else
                        trySend(Resource.Error("Check your internet connection."))

                }.addOnFailureListener {
                    trySend(Resource.Error("Check your internet connection."))
                }
            awaitClose { }
        }

    fun markChatAsRead(chatID: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .update("unReadCounter", 0)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Chat is marked as read successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun getMessage(messageID: String, chatID: String) = callbackFlow<Resource<ChatMessage>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .collection("Messages").document(messageID).get().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success(it.result.toObject(ChatMessage::class.java)!!))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }
        awaitClose()
    }

    fun deleteLastMessage(chatID: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .update("lastMessage", Constants.DELETED_MESSAGE_CODE).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Last message deleted successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }

        awaitClose()
    }


    fun getUserFCMToken() = callbackFlow<Resource<String>> {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("User FCM token: ", it.result.toString())
                trySend(Resource.Success(it.result.toString()))
            } else
                trySend(Resource.Error(it.exception!!.message.toString()))

        }
        awaitClose { }
    }

    fun storeFCMToken(userId: String, token: String) = callbackFlow<Resource<String>> {
        reference.document(userId).update("token", token).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("FCM Token stored successfully."))
            else
                trySend(Resource.Error("Error while storing FCM Token: ${it.exception!!.message.toString()}"))
        }


        awaitClose {}
    }

    fun uploadStoryView(storyID: String, view: StoryView) = callbackFlow<Resource<String>> {
        val ref = FirebaseFirestore.getInstance().collection("Stories").document(storyID)
            .collection("Views").document(view.uid!!)
        view.id = ref.id
        ref.set(view).addOnCompleteListener {
            if (it.isSuccessful)
                trySend(Resource.Success("view uploaded."))
            else
                trySend(Resource.Error(it.exception!!.message.toString()))

        }
        awaitClose()
    }

    fun getStoryViews(storyID: String) = callbackFlow<Resource<MutableList<StoryView>>> {
        val listener = FirebaseFirestore.getInstance().collection("Stories").document(storyID)
            .collection("Views")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("chat is empty"))
                    return@addSnapshotListener
                }
                if (!value.isEmpty) {
                    trySend(Resource.Success(value.toObjects(StoryView::class.java)))
                }
            }
        awaitClose {
            listener.remove()
        }
    }

    fun getUsersByName(username: String) = callbackFlow<Resource<MutableList<User>>> {
        val listener = FirebaseFirestore.getInstance().collection("Users")
            .orderBy("fullName").startAt(username).endAt(username + "\uf8ff")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("chat is empty"))
                    return@addSnapshotListener
                }
                if (!value.isEmpty) {
                    trySend(Resource.Success(value.toObjects(User::class.java)))
                }
            }

        awaitClose { listener.remove() }
    }

    fun deleteMessage(id: String, chatID: String) = callbackFlow<Resource<String>> {
        msgReference.document(chatID).collection("Messages").document(id)
            .update("messageType", 6).addOnCompleteListener {
                if (it.isSuccessful) {
                    trySend(Resource.Success("Message Deleted."))
                } else {
                    trySend(Resource.Error(it.exception!!.message.toString()))
                }
            }

        awaitClose()
    }

    fun editMessage(id: String, chatID: String, message: String) = callbackFlow<Resource<String>> {
        msgReference.document(chatID).collection("Messages").document(id)
            .update("editedText", message).addOnCompleteListener {
                if (it.isSuccessful) {
                    trySend(Resource.Success("Message Deleted."))
                } else {
                    trySend(Resource.Error(it.exception!!.message.toString()))
                }
            }
        awaitClose()
    }

    fun getUserNotifications(userId: String) = callbackFlow<Resource<MutableList<Notification>>> {
        trySend(Resource.Loading())
        val listener = reference.document(userId).collection("Notifications")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Notifications is empty"))
                    return@addSnapshotListener
                }

                if (!value.isEmpty) {
                    trySend(Resource.Success(value.toObjects(Notification::class.java)))
                } else {
                    trySend(Resource.Error("Notifications is empty"))
                    return@addSnapshotListener
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    fun updateUserNotifications(userId: String, notification: Notification) =
        callbackFlow<Resource<String>> {
            val doc = reference.document(userId).collection("Notifications")
            notification.id = doc.document().id
            reference.document(userId).collection("Notifications").document(notification.id)
                .set(notification).addOnCompleteListener {
                    if (it.isSuccessful) {
                        trySend(Resource.Success("user notification list updated."))
                    } else
                        trySend(Resource.Error(it.exception!!.message.toString()))

                }
            awaitClose { }
        }

    fun deleteUserNotification(userId: String, id: String, postUrl: String) =
        callbackFlow<Resource<String>> {
            val query = FirebaseFirestore.getInstance().collection("Users").document(userId)
                .collection("Notifications")
            query.whereEqualTo("uid", id)
            query.whereEqualTo("postUrl", postUrl)
            query.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val notification = it.result.toObjects(Notification::class.java)
                    if (notification.isNotEmpty()) {
                        reference.document(userId).collection("Notifications")
                            .document(notification[0].id).delete()
                        trySend(Resource.Success("Deleting notification done successfully."))
                    }
                } else
                    trySend(Resource.Success(it.exception!!.message.toString()))
            }
            awaitClose()
        }

    fun editProfile(user: User) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        reference.document(user.id).set(user, SetOptions.merge()).addOnCompleteListener {
            if (it.isSuccessful) {
                trySend(Resource.Success("User data updated successfully."))
            } else {
                trySend(Resource.Error(it.exception!!.message.toString()))
            }
        }

        awaitClose()
    }

    fun changeUserPassword(oldPassword: String, newPassword: String, email: String) =
        callbackFlow<Resource<String>> {
            trySend(Resource.Loading())
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val credentials = EmailAuthProvider.getCredential(email, oldPassword)
            firebaseUser!!.reauthenticate(credentials).addOnCompleteListener {
                if (it.isSuccessful) {
                    firebaseUser.updatePassword(newPassword).addOnCompleteListener {
                        if (it.isSuccessful)
                            trySend(Resource.Success("Your password changed successfully."))
                        else
                            trySend(Resource.Error("Something went wrong."))
                    }
                } else {
                    trySend(Resource.Error("Your current password in invalid!"))
                }
            }
            awaitClose()
        }

    fun resetPasswordViaEmail(email: String) = callbackFlow<Resource<String>> {
        val auth = FirebaseAuth.getInstance()
        trySend(Resource.Loading())
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            if (it.isSuccessful) {
                trySend(Resource.Success("Reset password link has been sent to your email"))
            } else {
                trySend(Resource.Error("Please enter correct email."))
            }
        }
        awaitClose()
    }

    fun updateMessageReact(chatID: String, messageID: String, react: String) =
        callbackFlow<Resource<String>> {
            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                .collection("Messages")
                .document(messageID).update("react", react)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        trySend(Resource.Success("React done successfully."))
                    else
                        trySend(Resource.Error(it.exception!!.message.toString()))
                }

            awaitClose()
        }

    fun getUserPost(postId: String) = callbackFlow {
        trySend(Resource.Loading())
        val listener = FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("chat is empty"))
                    return@addSnapshotListener
                }
                if (value.exists()) {
                    val post = value.toObject(PostImage::class.java)
                    trySend(Resource.Success(post!!))
                }
            }
        awaitClose { listener.remove() }
    }

    fun updatePostsLike(post: PostImage) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Post Images").document(post.id!!)
            .set(post, SetOptions.merge()).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("like added successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }

        awaitClose()
    }

    fun getUsersPosts(idList: ArrayList<String>) = callbackFlow<Resource<MutableList<PostImage>>> {
        FirebaseFirestore.getInstance().collection("Post Images").whereIn("uid", idList).get()
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success(it.result.toObjects(PostImage::class.java)))
                else
                    trySend(Resource.Error("chat is empty"))

                if (it.result.size() > 0) {
                    lastResult = it.result.documents[it.result.size() - 1]
                }
            }
        awaitClose()
    }

    fun requestDeleteChat(chatID: String, userId: String) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .update("deleteRequest", userId)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Chat delete request submitted."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun getChat(chatID: String) = callbackFlow {
        val listener = FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Chat is empty"))
                    return@addSnapshotListener
                }
                val chat = value.toObject(ChatUser::class.java)
                trySend(Resource.Success(chat!!))
            }

        awaitClose { listener.remove() }
    }

    fun deleteChatPermanently(chatID: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .delete().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Chat deleted successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun hideChat(chatID: String, userId: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val chat = task.result.toObject(ChatUser::class.java)
                    var hideIDS = chat!!.hideIDS
                    hideIDS.add(userId)
                    FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                        .update("hideIDS", hideIDS).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Chat is hidden successfully."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))
                        }
                } else
                    trySend(Resource.Error(task.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun showChat(chatID: String, hideIDS: ArrayList<String>) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .update("hideIDS", hideIDS).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Chat is shown successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }

        awaitClose()
    }

    fun getHiddenMessage(userId: String) = callbackFlow<Resource<MutableList<ChatUser>>> {
        val listener = FirebaseFirestore.getInstance().collection("Messages")
            .whereArrayContains("hideIDS", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                if (value == null) {
                    trySend(Resource.Error("Chat is empty"))
                    return@addSnapshotListener
                }
                val chats = value.toObjects(ChatUser::class.java)
                trySend(Resource.Success(chats))
            }
        awaitClose { listener.remove() }
    }

    fun deleteChat(chatID: String, userId: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val chat = task.result.toObject(ChatUser::class.java)
                    var deleteIDS = chat!!.hideIDS
                    deleteIDS.add(userId)
                    FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                        .update("deleteIDS", deleteIDS).addOnCompleteListener {
                            if (it.isSuccessful)
                                trySend(Resource.Success("Chat is hidden successfully."))
                            else
                                trySend(Resource.Error(it.exception!!.message.toString()))
                        }
                } else
                    trySend(Resource.Error(task.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun unDeleteChat(chatID: String, deleteIDS: ArrayList<String>) =
        callbackFlow<Resource<String>> {
            FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                .update("deleteIDS", deleteIDS).addOnCompleteListener {
                    if (it.isSuccessful)
                        trySend(Resource.Success("Chat is unDeleted successfully."))
                    else
                        trySend(Resource.Error(it.exception!!.message.toString()))
                }
            awaitClose()
        }

    fun updateProfilePrivacy(userId: String, type: Boolean) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .update("isprivate", type).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Profile privacy changed successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun updateGroupData(chatID: String, chatUser: ChatUser) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
            .set(chatUser, SetOptions.merge()).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("group data updated successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun updateGroupImage(chatID: String, uri: Uri) = callbackFlow<Resource<String>> {
        trySend(Resource.Loading())
        FirebaseStorage.getInstance().reference.child("Profile Pictures")
            .putFile(uri).addOnCompleteListener {
                it.result.storage.downloadUrl.addOnCompleteListener { uri ->
                    if (uri.isSuccessful) {
                        FirebaseFirestore.getInstance().collection("Messages").document(chatID)
                            .get().addOnCompleteListener { documentSnapshotTask ->
                                if (documentSnapshotTask.isSuccessful) {
                                    val chat =
                                        documentSnapshotTask.result.toObject(ChatUser::class.java)
                                    chat!!.groupProfileImg = uri.result.toString()
                                    FirebaseFirestore.getInstance().collection("Messages")
                                        .document(chatID)
                                        .set(chat, SetOptions.merge())
                                    trySend(Resource.Success("Group profile updated."))
                                } else
                                    trySend(Resource.Error(documentSnapshotTask.exception!!.message.toString()))
                            }
                    } else
                        trySend(Resource.Error(it.exception!!.message.toString()))
                }
            }
        awaitClose()
    }

    fun updatePostHideIds(postId: String, hideIDS: List<String>) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .update("hideIds", hideIDS).addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Post hided successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun updateUserSavedPosts(userId: String, postsIds: List<String>) =
        callbackFlow<Resource<String>> {
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                .update("savedPosts", postsIds).addOnCompleteListener {
                    if (it.isSuccessful)
                        trySend(Resource.Success("Post Saved successfully."))
                    else
                        trySend(Resource.Error(it.exception!!.message.toString()))
                }
            awaitClose()
        }

    fun getUsersPostsByID(idList: ArrayList<String>) =
        callbackFlow<Resource<MutableList<PostImage>>> {
            idList.add("")
            FirebaseFirestore.getInstance().collection("Post Images").whereIn("id", idList).get()
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        trySend(Resource.Success(it.result.toObjects(PostImage::class.java)))
                    else
                        trySend(Resource.Error("chat is empty"))

                    if (it.result.size() > 0) {
                        lastResult = it.result.documents[it.result.size() - 1]
                    }
                }
            awaitClose()
        }

    fun deletePost(postId: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Post Images").document(postId)
            .delete().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Post deleted."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))
            }
        awaitClose()
    }

    fun deleteStory(storyID: String) = callbackFlow<Resource<String>> {
        FirebaseFirestore.getInstance().collection("Stories").document(storyID)
            .delete().addOnCompleteListener {
                if (it.isSuccessful)
                    trySend(Resource.Success("Story deleted successfully."))
                else
                    trySend(Resource.Error(it.exception!!.message.toString()))

            }
        awaitClose()
    }
}