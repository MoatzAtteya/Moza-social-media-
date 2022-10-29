package com.example.moza.models

data class ChatUser(
    var id: String? = "",
    var uid: ArrayList<String> = ArrayList(),
    var hideIDS: ArrayList<String> = ArrayList(),
    var lastMessage: String? = "",
    var timeStamp: Long? = 0,
    var deleteRequest : String = "",
    var unReadCounter : Int = 0,
    var unReadSender : String = "",
    var deleteIDS : ArrayList<String> = ArrayList(),
    var isGroupChat : Boolean = false,
    var groupProfileImg : String = "",
    var groupTitle : String = "",
    var adminID : String = "",
    var lastMessage_id : String = ""
)
