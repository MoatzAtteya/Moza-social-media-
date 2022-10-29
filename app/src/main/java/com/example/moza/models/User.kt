package com.example.moza.models

import java.io.Serializable
import java.util.*

data class User(
    var fullName : String = "",
    var username : String = "",
    val email : String = "",
    val profilePicture : String = "",
    val id : String = "",
    var followers : ArrayList<String> = ArrayList(),
    var following : ArrayList<String> = ArrayList(),
    var status : String ?="",
    var online: Boolean? = false,
    var token : String ?= "",
    var notificationID : Int ?= Random().nextInt(),
    var isprivate : Boolean = true,
    var savedPosts : ArrayList<String> = ArrayList()

    ) : Serializable
