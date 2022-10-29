package com.example.moza.models

data class Notification(
    var id : String = "",
    var username : String = "",
    val postUrl : String = "",
    val uid : String = "",
    val type : String = "",
    val time : Long = 0,
    val postID : String = ""
)
