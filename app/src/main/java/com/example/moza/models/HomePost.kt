package com.example.moza.models

import java.util.*
import kotlin.collections.ArrayList

data class HomePost(
    var userName : String ?= "",
    var timeStamp : Date ?= null,
    var profileImage : String ?= "",
    var imageUrl : String ?= "",
    var id : String ?= "",
    var comments : String ?= "",
    var description : String ?= "",
    var likes : ArrayList<String> = ArrayList(),
    var uid : String = ""

)
