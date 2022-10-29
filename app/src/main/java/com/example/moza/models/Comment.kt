package com.example.moza.models

import java.util.*

data class Comment(
    var uid: String? = "",
    var postID: String? = "",
    var commentID: String? = "",
    var comment: String? = "",
    var timeStamp: Long = 0
)
