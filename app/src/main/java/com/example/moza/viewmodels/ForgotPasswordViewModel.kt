package com.example.moza.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moza.common.Resource
import com.google.firebase.auth.FirebaseAuth


class ForgotPasswordViewModel : ViewModel() {

    val auth = FirebaseAuth.getInstance()
    val resetPasswordResponse : MutableLiveData<Resource<String>> = MutableLiveData()

    fun resetPasswordViaEmail(email : String) {
        resetPasswordResponse.postValue(Resource.Loading())
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            if (it.isSuccessful){
                resetPasswordResponse.postValue(Resource.Success("Reset password link has been sent"))
            } else {
                resetPasswordResponse.postValue(Resource.Error("Please enter correct email."))
            }
        }
    }

}