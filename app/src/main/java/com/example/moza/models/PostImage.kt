package com.example.moza.models

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

data class PostImage(
    var imageUrl: String? = "",
    var id: String? = "",
    @ServerTimestamp
    var timeStamp : Date ?= null,
    var description : String ?= "",
    var profileImage : String ?= null,
    var userName : String ?="",
    var likes : ArrayList<String> = ArrayList(),
    var uid : String = "",
    var hideIds : ArrayList<String> = ArrayList()
) : Serializable
