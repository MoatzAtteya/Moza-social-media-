package com.example.moza.common

/*
    this Generic class is recommended by google to wrap around our response
    and handle our success and error states and loading state
 */
sealed class Resource<T>(
    val data : T? = null,
    val message : String? = null
) {

    class Success<T>(data: T) : Resource<T>(data)

    class Error<T>(message: String , data: T? = null) : Resource<T>(data , message)

    class Loading<T> : Resource<T>()


}