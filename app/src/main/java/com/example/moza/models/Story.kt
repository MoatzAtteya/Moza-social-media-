package com.example.moza.models

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

data class Story(
    var videoUrl: String? = "",
    val username: String? = "",
    var id: String? = "",
    val uid: String? = "",
    val profileIMGURL: String? = "",
    @ServerTimestamp
    var timeStamp : Date?= null,
) : Serializable
