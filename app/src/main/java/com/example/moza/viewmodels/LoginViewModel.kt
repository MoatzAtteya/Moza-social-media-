package com.example.moza.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginInViewModel @Inject constructor(
    val repository: FireBaseRepository,
    @ApplicationContext val context: Context
) : ViewModel(){

    val loginFirebaseResponse : MutableLiveData<Resource<String>> = MutableLiveData()
    var createAccountResponse: MutableLiveData<Resource<String>> = MutableLiveData()

    private var _googleSignInResponse: MutableLiveData<Resource<FirebaseUser>> = MutableLiveData()
    val googleSignInResponse: MutableLiveData<Resource<FirebaseUser>> get() = _googleSignInResponse




    fun firebaseUserLogin(email : String , password : String) = viewModelScope.launch {
        safeUserLogin(email, password)
    }

    fun createAccount(user: User) = viewModelScope.launch {
        safeCreateAccount(user)
    }

    suspend fun safeUserLogin(email: String , password: String) {
        repository.userLogin(email, password).collect{ response->
            when(response){
                is Resource.Error ->loginFirebaseResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> loginFirebaseResponse.postValue(Resource.Loading())
                is Resource.Success ->loginFirebaseResponse.postValue(Resource.Success(response.data!!))

            }
        }
    }

    private  suspend fun safeCreateAccount(user: User) {
        repository.uploadUserData2(user , context).collect{ response->
            when(response){
                is Resource.Error ->createAccountResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> {}
                is Resource.Success ->createAccountResponse.postValue(Resource.Success(response.data!!))

            }
        }
    }

    fun googleSignIn(idToken : String) = viewModelScope.launch {
        repository.googleLogIn(idToken).collect{response->
            when(response){
                is Resource.Error -> googleSignInResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> TODO()
                is Resource.Success -> googleSignInResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

}

