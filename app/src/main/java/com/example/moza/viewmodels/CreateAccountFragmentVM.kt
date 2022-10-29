package com.example.moza.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountFragmentVM @Inject constructor( val repo: FireBaseRepository) :
    ViewModel() {

    var createAccountResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    // getting the firebase response from the repo.
    fun createAccount(user: User , password : String) = viewModelScope.launch {
        safeCreateAccount(user , password)
    }

         /*
           handling the firebase response and if success,
            get uploadUserData response from the firebase
         */
    private suspend fun safeCreateAccount(user: User,password : String) {
        createAccountResponse.postValue(Resource.Loading())
        repo.registerUserFirebase(user,password).collect { response ->
            when (response) {
                is Resource.Loading -> {
                    createAccountResponse.postValue(Resource.Loading())
                }
                is Resource.Error -> {
                    createAccountResponse.postValue(Resource.Error(response.message!!))
                }
                is Resource.Success -> {
                    createAccountResponse.postValue(Resource.Success("done"))
                }
            }

        }

    }

}
