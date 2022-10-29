package com.example.moza.models

import java.io.Serializable

data class ChatMessage(
    var id: String? = "",
    var senderID: String ?= "",
    var message: String? = "",
    var time: Long? = 0,
    var deleted : Boolean =false,
    var editedText : String ?= "",
    var messageType : Int = 0,
    var isForwarded : Boolean = false,
    var messageUrl : String ?= "",
    var react : String = "",
    var replyingText : String = "",
    var replyTo : String = "",
    var replyType : Int = 0,

) : Serializable
